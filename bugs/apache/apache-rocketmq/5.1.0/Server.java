import io.netty.buffer.ByteBuf;
import org.apache.rocketmq.common.namesrv.NamesrvConfig;
import org.apache.rocketmq.namesrv.NamesrvController;
import org.apache.rocketmq.namesrv.processor.DefaultRequestProcessor;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.remoting.netty.NettyServerConfig;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.nio.ByteBuffer;

public class Server {
    public static void main(String[] args) throws RemotingCommandException {
        NamesrvController namesrvController = new NamesrvController(new NamesrvConfig(), new NettyServerConfig());

        DefaultRequestProcessor defaultRequestProcessor = new DefaultRequestProcessor(namesrvController);

        RemotingCommand remotingCommand = RemotingCommand.decode(getSource());
        defaultRequestProcessor.processRequest(null, remotingCommand);
//        byte[] bs = remotingCommand.getBody();
//        byte b = bs[1];
    }

    public static byte[] getSource(){
        byte[] bs = new byte[5];
        bs[0] = getTaint();
        return bs;
    }

    public static byte getTaint(){
        return 0;
    }

    public static void sink(byte b){

    }
}
