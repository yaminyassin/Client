import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import rpcstubs.ConfigServiceGrpc;
import rpcstubs.Empty;
import rpcstubs.ServerInfo;

import java.util.Scanner;


public class Client2 {

    private ManagedChannel configChannel;
    private ConfigServiceGrpc.ConfigServiceStub configStub;
    private String serverIP;
    private int serverPort;

    public Client2(String configIP, int configPort){

        configChannel = ManagedChannelBuilder
                .forAddress(configIP, configPort)
                .usePlaintext()
                .build();

        configStub = ConfigServiceGrpc
                .newStub(configChannel);

        configStub.getClusterInfo(Empty.newBuilder().build(),
                new StreamObserver<>() {
                    @Override
                    public void onNext(ServerInfo serverInfo) {
                        System.out.println("------------ON NEXT---------");
                        serverIP = serverInfo.getIp();
                        serverPort = serverInfo.getPort();
                        System.out.println("got server details { "
                                + serverIP + ", " + serverPort + "}");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.err.println(Status.UNKNOWN);

                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("got server details! Completed");
                    }

                });

    }

    public static void main(String[] args) {
        Client2 client2 = new Client2("192.168.1.250", 5050);
        Scanner sc = new Scanner(System.in);

        sc.nextInt();
    }
}
