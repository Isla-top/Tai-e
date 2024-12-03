import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.zip.transformer.UnZipTransformer;
import org.springframework.messaging.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class Server {
    private static final ResourceLoader resourceLoader = new DefaultResourceLoader();

    public static void main(String[] args) {
        final Resource evilResource = resourceLoader.getResource(getInput());
        try{
            //InputStream evilIS = evilResource.getInputStream();
            InputStream evilIS = getInputStream(evilResource);
            Message<InputStream> evilMessage = MessageBuilder.withPayload(evilIS).build();
            UnZipTransformer unZipTransformer = new UnZipTransformer();
            unZipTransformer.transform(evilMessage);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    public static String getInput(){
        char[] chs = "classpath:poc.zip".toCharArray();
        chs[0] = taint();
        return new String(chs);
    }

    public static char taint(){
        return 'c';
    }

    public static InputStream getInputStream(Resource resource) throws IOException {
        return new FileInputStream(resource.getFilename());
    }

}
