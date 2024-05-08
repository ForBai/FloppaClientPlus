package floppaclient.tweaker;

import com.sun.jna.Native;
import gg.essential.loader.stage0.EssentialSetupTweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.fusesource.hawtjni.runtime.JNIEnv;

import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketImpl;

@SuppressWarnings("unused")
public class FloppaClientTweaker extends EssentialSetupTweaker {

    public static LaunchClassLoader launchClassLoader;

    public FloppaClientTweaker() throws MalformedURLException {
        super();
//
        //change the constructor of the java.net.URL class to use our own URL class
        //attach following agent here: /home/lars/dumper.jar

    }


    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        super.injectIntoClassLoader(classLoader);
        launchClassLoader = classLoader;

    }

}

/********** for sbe
 //        HttpsURLConnectionImpl
 *********/


/* propterties
user.name
os.version
os.name
user.home
 */

/* env vars
PROCESS_IDENTIFIER
PROCESSOR_LEVEL
PROCESSOR_REVISION
PROCESSOR_ARCHITECTURE
PROCESSOR_ARCHITEW6432
NUMBER_OF_PROCESSORS
COMPUTERNAME
os
os.name
 */
//    public static String encrypt(String toEncrypt, String algorithm) {
//        try {
//            MessageDigest md = MessageDigest.getInstance(algorithm);
//            md.update(toEncrypt.getBytes());
//            StringBuffer hexString = new StringBuffer();
//
//            byte byteData[] = md.digest();
//
//            for (byte aByteData : byteData) {
//                String hex = Integer.toHexString(0xff & aByteData);
//                if (hex.length() == 1) hexString.append('0');
//                hexString.append(hex);
//            }
//
//            return hexString.toString();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Error";
//        }
//    }
/*
"-DproxyHost=127.0.0.1"
"-DproxyPort=8080"
 */