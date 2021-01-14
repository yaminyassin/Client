package Client;

import io.grpc.*;
import io.grpc.stub.StreamObserver;
import rpcstubs.*;
import rpcstubs.Void;
import rpcstubs.Empty;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client {

    //vars do configService
    private ManagedChannel configChannel;
    private ConfigServiceGrpc.ConfigServiceStub configStub;

    //vars do storageService
    private ManagedChannel storageChannel;
    private StorageServiceGrpc.StorageServiceBlockingStub storageBStub;
    public static String serverIP;
    public static int serverPort;
    public static boolean obteveLigacao;

    private Scanner scanner;
    private int estado;

    public Client(String configIP, int configPort){

        this.scanner = new Scanner(System.in);
        this.estado = 0;
        obteveLigacao = false;

        configChannel = ManagedChannelBuilder
                .forAddress(configIP, configPort)
                .usePlaintext()
                .build();

        configStub = ConfigServiceGrpc
                .newStub(configChannel);
    }

    public void menu(){
        while(estado!= 99){
            switch(estado){
                default: break;

                case 0: // ----------- Menu Before Connection
                    showMenuBeforeConn();
                    escolhaEstado();

                    if(this.estado != 1 && this.estado != 99 ){
                        System.out.println("Invalid Option!");
                        this.estado = 0;
                    }

                    break;

                case 1: // ----------- Get Server Configuration
                    obteveLigacao = false;

                    this.getClusterGroup();
                    System.out.println("Getting Cluster Group! Please wait..." + "\n");
                    this.estado = 2;
                    break;

                case 2:
                    try {
                        Thread.sleep(400);
                        if(obteveLigacao){
                            this.estado = 3;
                        }

                    } catch (InterruptedException e) {
                       System.err.println("Error on case 2");
                    }
                    break;

                case 3: // ----------- MENU AFTER CONNECTION

                    storageChannel = ManagedChannelBuilder
                            .forAddress(serverIP, serverPort)
                            .usePlaintext()
                            .build();

                    storageBStub =  StorageServiceGrpc
                            .newBlockingStub(storageChannel);

                    showMenuAfterConn();
                    escolhaEstado();

                    if(this.estado != 4 && this.estado != 5 && this.estado != 99 ){
                        System.out.println("Invalid Option!");
                        this.estado = 3;
                    }
                    break;

                case 4:  // ----------- WRITE
                    System.out.print("Write Key -> "); String chave1 = this.scanner.next();
                    System.out.print("Write Value -> "); String valor1 = this.scanner.next();
                    System.out.println("Sending data -> (" + chave1 + ", " + valor1 + ")");

                    this.sendMsg(chave1, valor1);  //grcp SEND MESSAGE

                    this.storageChannel.shutdown();
                    this.estado = 1;
                    break;

                case 5: // ----------- READ
                    System.out.print("Which Key do you want to read? -> ");
                    String chave2 = this.scanner.next();
                    System.out.println("Requesting Value from Key -> " + chave2);

                    Valor resultado = this.readMsg(chave2); //grcp READ MESSAGE

                    this.storageChannel.shutdown();
                    this.estado = 1;
                    break;
            }
        }
        this.storageChannel.shutdown();
    }

    private void sendMsg(String key, String value){
        Chave chave = Chave.newBuilder().setValue(key).build();
        Valor valor = Valor.newBuilder().setValue(value).build();

        try{
            Void msg = storageBStub.write(Par
                    .newBuilder()
                    .setChave(chave)
                    .setValor(valor)
                    .build() );
        }catch (StatusRuntimeException e){
            System.err.println("Servidor nao responde..Pedindo novo servidor");
            this.getClusterGroup();
        }
    }

    private Valor readMsg(String key){
        try{
            Valor valor= storageBStub.read(Chave
                    .newBuilder()
                    .setValue(key)
                    .build());

            switch (valor.getResultadoCase()){
                case VALUE:
                    System.out.println("Key " + key + " has Value " + valor.getValue() + ".");
                    break;
                case VOID:
                    System.out.println("Chave nao existe");
            }
            return valor;

        }catch (StatusRuntimeException e){
            System.err.println("Servidor nao responde..Pedindo novo servidor");
            this.getClusterGroup();


            return Valor.newBuilder()
                    .setVoid(Void.newBuilder().build())
                    .build();
        }
    }

    private void getClusterGroup(){
        configStub.getClusterInfo(Empty.newBuilder().build(),
            new StreamObserver<>() {

                @Override
                public void onNext(Resposta resposta) {
                    switch (resposta.getInfoCase()){

                        case EMPTY:
                            obteveLigacao = false;
                            System.out.println("No Server Avaliable...Placed on Waiting List...");
                            estado = 2;
                            break;

                        case SERVERINFO:
                            serverIP = resposta.getServerInfo().getIp();
                            serverPort = resposta.getServerInfo().getPort();
                            System.out.println("got server details { "
                                    + serverIP + ", " + serverPort + "}");
                            estado = 2;
                            obteveLigacao = true;
                            break;
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    System.err.println("Error Connectiong to Configuration Service");
                    estado = 0;
                }

                @Override
                public void onCompleted() {
                    System.out.println("got server details! Completed");
                }

            });
    }

    private void showMenuBeforeConn(){
        System.out.println();
        System.out.println("    MENU");
        System.out.println(" 1 - Get Server Connection");
        System.out.println("99 - Exit");
        System.out.println();
        System.out.println("Choose an Option?");
    }

    private void showMenuAfterConn(){
        System.out.println();
        System.out.println("    MENU");
        System.out.println(" 4 - Write");
        System.out.println(" 5 - Read");
        System.out.println("99 - Exit");
        System.out.println();
        System.out.print("Choose -> ");
    }

    private void escolhaEstado(){
        try{
            this.estado = this.scanner.nextInt();
        }catch (InputMismatchException e){
            System.err.println("Input Mismatch");
            this.scanner = new Scanner(System.in);
            this.estado = 0;
        }

    }
    public static void main(String[] args) {
        String configIP = "192.168.1.250";
        int configPort = 5050;

        if(args.length >0){
            configIP = args[0];
            configPort = Integer.parseInt(args[1]);
        }
       Client client = new Client(configIP,configPort);
       client.menu();
    }
}
