package keyp.forev.fmc.common.util;

import java.security.SecureRandom;
import java.util.Random;

public class OTPGenerator {
  private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final SecureRandom random = new SecureRandom();

  public static String generateOTP(int length) {
    StringBuilder otp = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      int index = random.nextInt(CHARACTERS.length());
      otp.append(CHARACTERS.charAt(index));
    }
    return otp.toString();
  }

  public static int generateOTPbyInt() {
    Random rnd = new Random();
    return (100000 + rnd.nextInt(900000));
  }
}
