import com.android.apksig.ApkSigner;

import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 使用 apksig 库为 APK 生成 v1 + v2 签名（替代缺失的 apksigner 命令行）。 */
public class SignApk {

    public static void main(String[] args) throws Exception {
        String ksPath = args[0];
        String ksPass = args[1];
        String alias = args[2];
        String keyPass = args[3];
        File input = new File(args[4]);
        File output = new File(args[5]);
        int minSdk = args.length > 6 ? Integer.parseInt(args[6]) : 24;

        KeyStore keyStore = KeyStore.getInstance(new File(ksPath), ksPass.toCharArray());
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, keyPass.toCharArray());
        List<X509Certificate> certs = new ArrayList<>();
        for (java.security.cert.Certificate cert : keyStore.getCertificateChain(alias)) {
            certs.add((X509Certificate) cert);
        }

        ApkSigner.SignerConfig config =
                new ApkSigner.SignerConfig.Builder(alias, privateKey, certs).build();

        new ApkSigner.Builder(Collections.singletonList(config))
                .setInputApk(input)
                .setOutputApk(output)
                .setMinSdkVersion(minSdk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .build()
                .sign();

        System.out.println("signed: " + output.getAbsolutePath()
                + " (" + output.length() + " bytes)");
    }
}
