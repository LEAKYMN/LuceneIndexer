/*
 * Copyright (C) 2018 Philip M. Trenwith
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package LuceneIndexer.cryptopackage;

import LuceneIndexer.cConfig;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.x509.X509V3CertificateGenerator;

/**
 *
 * @author Philip M. Trenwith
 */
public class cCryptographer 
{
  private static String g_sCERTIFICATE_SIGNING_ALGORITHM = "SHA256WithRSAEncryption";
  private static String g_sPBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";
  private static String g_sAES_ALGORITHM = "AES/CTR/PKCS5Padding";
  private static final String g_sINIT_VECTOR = "RandomInitVector";
  private static final String g_sPROVIDER = "BC"; // Bouncy Castle
  private static int iBUFFER_SIZE = 4096;
  private static byte [] yBUFFER = new byte[iBUFFER_SIZE];
  
  private cCryptographer()
  {
    System.out.println("Cryptographer initialized");
  }
  
  public static void symmetricEncryption(FileChannel oSourceChannel, String sAESSecretKey)
  {
    try
    {
      File oTempFile = File.createTempFile("Temp", null);
      RandomAccessFile oRandomAccessFile = new RandomAccessFile(oTempFile, "rw");
      FileChannel oTempChannel = oRandomAccessFile.getChannel();

      byte[] yAESKey = StringToBase64DecodedBytes(sAESSecretKey);
      ByteBuffer oReadBuffer = ByteBuffer.allocate(iBUFFER_SIZE);
      int iRead = oSourceChannel.read(oReadBuffer);
      oReadBuffer.rewind();
      
      while (iRead > -1)
      {
        byte[] plain = new byte[iRead];
        oReadBuffer.get(plain, 0, iRead);
        
        byte[] cipher = symmetricEncryption(plain, yAESKey);
        ByteBuffer oWriteBuffer = ByteBuffer.wrap(cipher);
        oTempChannel.write(oWriteBuffer);
        
        oReadBuffer.clear();
        iRead = oSourceChannel.read(oReadBuffer);
        oReadBuffer.rewind();
      }

      oRandomAccessFile.close();
      oTempChannel.close();
      oRandomAccessFile = new RandomAccessFile(oTempFile, "r");
      oTempChannel = oRandomAccessFile.getChannel();
      
      long size = oSourceChannel.size();
      oSourceChannel.truncate(0);
      oSourceChannel.transferFrom(oTempChannel, 0, size);

      oRandomAccessFile.close();
      oTempChannel.close();
      oTempFile.delete();
    }
    catch (IOException ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * Encrypt the given file. The file will be copied to the temp directory and a new encrypted file will be created in its place
   * @param oSourceFile The file to encrypt
   * @param sAESSecretKey The encryption key
   * @return The handle to the encrypted file
   */
  public static File symmetricEncryption(File oSourceFile, String sAESSecretKey)
  {
    File oDestinationFile;
    try
    {
      oDestinationFile = File.createTempFile(oSourceFile.getName(), null);
      oDestinationFile.delete();
      boolean bRenamed=oSourceFile.renameTo(oDestinationFile);
      if (!bRenamed)
      {
        throw new RuntimeException("could not rename the file "+oSourceFile.getAbsolutePath()+" to "+oDestinationFile.getAbsolutePath());
      }
      
      try 
      {
        byte[] yAESKey = StringToBase64DecodedBytes(sAESSecretKey);
        FileInputStream is = new FileInputStream(oDestinationFile.getAbsolutePath());
        FileOutputStream os = new FileOutputStream(oSourceFile.getAbsolutePath());
        byte[] yBuffer = new byte[iBUFFER_SIZE];
        int iRead = is.read(yBuffer);
        
        while (iRead > -1)
        {
          if (iRead != iBUFFER_SIZE)
          {
            byte[] yTemp = new byte[iRead];
            System.arraycopy(yBuffer, 0, yTemp, 0, iRead);
            yBuffer = yTemp;
          }
          
          byte[] cipher = symmetricEncryption(yBuffer, yAESKey);
          os.write(cipher);
          iRead = is.read(yBuffer);
        }

        os.flush();
        os.close();
        is.close();
        oDestinationFile.delete();
      }
      catch (IOException ex) 
      {
        Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    catch (IOException ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }
    return oSourceFile;
  }
  
  public static void symmetricDecryption(FileChannel oSourceChannel, String sAESSecretKey)
  {
    try
    {      
      File oTempFile = File.createTempFile("Temp", null);
      RandomAccessFile oRandomAccessFile = new RandomAccessFile(oTempFile, "rw");
      FileChannel oTempChannel = oRandomAccessFile.getChannel();

      byte[] yAESKey = StringToBase64DecodedBytes(sAESSecretKey);
      ByteBuffer oReadBuffer = ByteBuffer.allocate(iBUFFER_SIZE);
      int iRead = oSourceChannel.read(oReadBuffer);
      oReadBuffer.rewind();
      
      while (iRead > -1)
      {
        byte[] cipher = new byte[iRead];
        oReadBuffer.get(cipher, 0, iRead);
        
        byte[] plain = symmetricDecryption(cipher, yAESKey);
        ByteBuffer oWriteBuffer = ByteBuffer.wrap(plain);
        oTempChannel.write(oWriteBuffer);
        
        oReadBuffer.clear();
        iRead = oSourceChannel.read(oReadBuffer);
        oReadBuffer.rewind();
      }

      oRandomAccessFile.close();
      oTempChannel.close();
      oRandomAccessFile = new RandomAccessFile(oTempFile, "r");
      oTempChannel = oRandomAccessFile.getChannel();
      
      long size = oSourceChannel.size();
      oSourceChannel.truncate(0);
      oSourceChannel.transferFrom(oTempChannel, 0, size);

      oRandomAccessFile.close();
      oTempChannel.close();
      oTempFile.delete();
    }
    catch (IOException ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * Decrypt the given file. The file will be copied to the temp directory and a new decrypted file will be created in its place
   * @param oSourceFile The file to decrypt
   * @param sAESSecretKey The decryption key
   * @return The handle to the decrypted file
   */
  public static File symmetricDecryption(File oSourceFile, String sAESSecretKey)
  {
    File oDestinationFile;
    try
    {
      oDestinationFile = File.createTempFile(oSourceFile.getName(), null);
      oDestinationFile.delete();
      boolean bRenamed=oSourceFile.renameTo(oDestinationFile);
      if (!bRenamed)
      {
        throw new RuntimeException("could not rename the file "+oSourceFile.getAbsolutePath()+" to "+oDestinationFile.getAbsolutePath());
      }
      
      try 
      {
        byte[] yAESKey = StringToBase64DecodedBytes(sAESSecretKey);
        FileInputStream is = new FileInputStream(oDestinationFile.getAbsolutePath());
        FileOutputStream os = new FileOutputStream(oSourceFile.getAbsolutePath());
        byte[] yBuffer = new byte[iBUFFER_SIZE];
        int iRead = is.read(yBuffer);
        
        while (iRead > -1)
        {
          if (iRead != iBUFFER_SIZE)
          {
            byte[] yTemp = new byte[iRead];
            System.arraycopy(yBuffer, 0, yTemp, 0, iRead);
            yBuffer = yTemp;
          }
          byte[] cipher = symmetricDecryption(yBuffer, yAESKey);
          os.write(cipher);
          iRead = is.read(yBuffer);
        }

        os.flush();
        os.close();
        is.close();
        
        oDestinationFile.delete();
      } 
      catch (IOException ex) 
      {
        Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    catch (IOException ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }
    return oSourceFile;
  }
  
  /**
   * Encrypt a string
   * @param sPlaintext The string to encrypt
   * @param sAESSecretKey The encryption key
   * @return The encrypted string
   */
  public static String symmetricEncryption(String sPlaintext, String sAESSecretKey)
  {
    try
    {
      byte[] yPlainText = sPlaintext.getBytes(Charset.forName("UTF-8"));
      byte[] yKey = StringToBase64DecodedBytes(sAESSecretKey);
      byte[] yEncryptedBytes = symmetricEncryption(yPlainText, yKey);
      String sCipherText = BytesToBase64EncodedString(yEncryptedBytes);
      return sCipherText;
    }
    catch (Exception ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }
  }
  
  /**
   * Decrypt a string
   * @param sCipherText The string to decrypt
   * @param sAESSecretKey The decryption key
   * @return The decrypted string
   */
  public static String symmetricDecryption(String sCipherText, String sAESSecretKey)
  {
    try
    {
      byte[] yCipherText = StringToBase64DecodedBytes(sCipherText);
      byte[] yKey = StringToBase64DecodedBytes(sAESSecretKey);
      byte[] yDecryptedBytes = symmetricDecryption(yCipherText, yKey);
      String sPlainText = new String (yDecryptedBytes, Charset.forName("UTF-8"));
      return sPlainText;
    }
    catch (Exception ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }
  }
  
  /**
   * Encrypt a byte array
   * @param yPlaintext The byte array to encrypt
   * @param yAESSecretKey The encryption key
   * @return The encrypted byte array
   */
  public static byte[] symmetricEncryption(byte[] yPlaintext, byte[] yAESSecretKey)
  {
    byte[] yCipherText = null;
    try
    {      
      SecretKeySpec oSecretKeySpec = new SecretKeySpec(yAESSecretKey, "AES");
      IvParameterSpec oIvParameterSpec = new IvParameterSpec(g_sINIT_VECTOR.getBytes());
      
      Cipher cipher = Cipher.getInstance(g_sAES_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, oSecretKeySpec, oIvParameterSpec);
      
      yCipherText = cipher.doFinal(yPlaintext);
    }
    catch (NoSuchAlgorithmException | NoSuchPaddingException | 
            InvalidKeyException | InvalidAlgorithmParameterException | 
            IllegalBlockSizeException | BadPaddingException ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
    }
    return yCipherText;
  }
  
  /**
   * Decrypt a byte array
   * @param yCipherText The byte array to decrypt
   * @param yAESSecretKey The decryption key
   * @return The decrypted byte array
   */
  public static byte[] symmetricDecryption(byte[] yCipherText, byte[] yAESSecretKey)
  {
    byte[] yPlainText = null;
    try
    {
      byte yIv[] = new byte[16];
      
      SecureRandom secRandom = new SecureRandom() ;
      secRandom.nextBytes(yIv); // self-seeded randomizer to generate IV
      
      SecretKeySpec oSecretKeySpec = new SecretKeySpec(yAESSecretKey, "AES");
      IvParameterSpec oIvParameterSpec = new IvParameterSpec(g_sINIT_VECTOR.getBytes());
      
      Cipher cipher = Cipher.getInstance(g_sAES_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, oSecretKeySpec, oIvParameterSpec);
      
      yPlainText = cipher.doFinal(yCipherText);
    }
    catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | 
            NoSuchPaddingException | InvalidKeyException | 
            IllegalBlockSizeException | BadPaddingException ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
    }
    return yPlainText;
  }
  
  /**
   * Generate an AES key of 256 bits
   * @return a AES-256 SecretKey 
   */
  public static byte[] generateAESSecretKey()
  {
    return generateAESSecretKey(256);
  }
  
  /**
   * Generate a AES key of the given size
   * @param keysize the size of the key. Eg. 128-bit, 256-bit
   * @return 
   */
  public static byte[] generateAESSecretKey(int keysize)
  {
    byte[] yReturn = null;
    try
    {
      KeyGenerator keygen = KeyGenerator.getInstance("AES") ; // key generator to be used with AES algorithm.
      keygen.init(keysize) ; // Key size is specified here.
      SecretKey generateKey = keygen.generateKey();
      yReturn = generateKey.getEncoded();
    }
    catch (NoSuchAlgorithmException ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
    }
    return yReturn;
  }
  
  /**
   * Derive an AES secret key from a given plaintext password and stored hash
   * @param keysize the size of the key. Eg. 128-bit, 256-bit
   * @param sPlaintextPassword the password, used for validating that the user has access to the key
   * @param sStoredSecureHash the output of generateSecurePassword(String sPassword) 
   * @return an AES key of length keysize
   */
  public static byte[] generateAESSecretKey(int keysize, String sPlaintextPassword, String sStoredSecureHash)
  {
    byte[] yReturn = null;
    try
    {
      if (validateSecurePassword(sPlaintextPassword, sStoredSecureHash))
      {
        String[] parts = sStoredSecureHash.split(":");
        byte[] salt = fromHex(parts[1]);
        byte[] hash = fromHex(parts[2]);
      
        SecretKeyFactory factory = SecretKeyFactory.getInstance(g_sPBKDF2_ALGORITHM);
        KeySpec spec = new PBEKeySpec(toHex(hash).toCharArray(), salt, 65536, keysize);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
        yReturn = secret.getEncoded();
      }
    }
    catch (NoSuchAlgorithmException | InvalidKeySpecException ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
    }
    return yReturn;
  }
  
  /**
   * @param keysize should be at least 2048
   * @return the generated RSA KeyPair
   */
  public static KeyPair generateAsymmetricKeyPair(int keysize)
  {
    KeyPair oKeyPair = null;
    try
    {
      KeyPairGenerator oKeyGenerator = KeyPairGenerator.getInstance("RSA");
      oKeyGenerator.initialize(keysize);
      //Generate the key pair
      oKeyPair = oKeyGenerator.generateKeyPair();
    }
    catch (NoSuchAlgorithmException ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
    }
    return oKeyPair;
  }
  
  /**
   * Generates a X509 Certificate from the KeyPair
   * Will use SHA256WithRSAEncryption algorithm and the Bouncy Castle Provider
   * @param oKeyPair the keypair to use
   * @return the X509Certificate
   */
  public static Certificate generateCertificate(KeyPair oKeyPair)
  {
    return generateCertificate(oKeyPair, g_sCERTIFICATE_SIGNING_ALGORITHM, g_sPROVIDER);
  }
  
  /**
   * Generates a X509 Certificate from the KeyPair 
   * Will use the Bouncy Castle Provider
   * @param oKeyPair the keypair to use
   * @param sAlgorithm the Algorithm to use
   * @return the X509Certificate
   */
  public static Certificate generateCertificate(KeyPair oKeyPair, String sAlgorithm)
  {
    return generateCertificate(oKeyPair, sAlgorithm, g_sPROVIDER);
  }
  
  /**
   * Generates a X509 Certificate from the KeyPair using the Bouncy Castle Provider
   * @param oKeyPair the keypair to use
   * @param sAlgorithm specify the algorithm to use
   * @param sProvider the provider to use for the algorithm - need to call Security.addProvider(instance); and add an 
   * instance of the provider before calling this method
   * @return the X509Certificate
   */
  public static Certificate generateCertificate(KeyPair oKeyPair, String sAlgorithm, String sProvider)
  {
    X509Certificate oX509Certificate = null;
    try
    {
      Security.addProvider(new BouncyCastleProvider());
      PrivateKey oPrivateKey = oKeyPair.getPrivate();
      PublicKey oPublicKey = oKeyPair.getPublic();
      SecureRandom oRandom = new SecureRandom();
      int iRandom = Math.abs(oRandom.nextInt());
      if (iRandom < 0)
      {
        iRandom++;
        iRandom = Math.abs(iRandom);
      }
      long lNow = new GregorianCalendar().getTimeInMillis();
      Date oBefore = new Date(lNow);
      Date oAfter = new Date(oBefore.getYear() + 1, oBefore.getMonth(), oBefore.getDate());
      X509V3CertificateGenerator cert = new X509V3CertificateGenerator();
      cert.setSerialNumber(BigInteger.valueOf(iRandom));
      cert.setSubjectDN(new X509Principal("CN=Philip Trenwith"));
      cert.setIssuerDN(new X509Principal("CN=Philip Trenwith"));
      cert.setPublicKey(oPublicKey);
      cert.setNotBefore(oBefore);
      cert.setNotAfter(oAfter);  
      cert.setSignatureAlgorithm(sAlgorithm);
      oX509Certificate = cert.generateX509Certificate(oPrivateKey, sProvider);
    }
    catch (NoSuchProviderException | SecurityException | 
            SignatureException | InvalidKeyException ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
    }
    return oX509Certificate;
  }
  

  /**
   * Generate a hashed password using the SecretKeyFactory and a random salt
   * @param sPassword the plaintext password to hash
   * @return the secure password in the format iterations:salt:hash when validating a user entered password call:
   * cCryptographer.validateSecurePassword(plaintext, stored hash);
   */
  public static String generateSecurePassword(String sPassword)
  {
    return generateSecurePassword(sPassword, 10000);
  }
  
  /**
   * Generate a hashed password using the SecretKeyFactory and a random salt specifying the iteration count
   * @param sPassword the plaintext password to hash
   * @param iterations the iterations for the PBEKey, it is recommended that this number is greater than 10000
   * @return the secure password in the format iterations:salt:hash when validating a user entered password call:
   * cCryptographer.validateSecurePassword(plaintext, stored hash);
   */
  public static String generateSecurePassword(String sPassword, int iterations)
  {
    String sReturn = null;
    try 
    {
      char[] chars = sPassword.toCharArray();
      byte[] salt = getSalt();

      PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
      SecretKeyFactory skf = SecretKeyFactory.getInstance(g_sPBKDF2_ALGORITHM);
      byte[] hash = skf.generateSecret(spec).getEncoded();

      sReturn = iterations + ":" + toHex(salt) + ":" + toHex(hash);
    }
    catch (NoSuchAlgorithmException | InvalidKeySpecException ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
    }
    return sReturn;
  }
  
  /**
   * Validate a password given the plaintext and the stored hash produced by cCryptographer.generateSecurePassword(String)
   * @param sPlaintextPassword The plaintext password as provided by the user
   * @param sStoredSecureHash The stored hash of the users password
   * @return true or false indicating if the password is a match
   */
  public static boolean validateSecurePassword(String sPlaintextPassword, String sStoredSecureHash) 
  {
    boolean bReturn = false;
    try
    {
      String[] parts = sStoredSecureHash.split(":");
      int iterations = Integer.parseInt(parts[0]);
      byte[] salt = fromHex(parts[1]);
      byte[] hash = fromHex(parts[2]);
      
      PBEKeySpec spec = new PBEKeySpec(sPlaintextPassword.toCharArray(), salt, iterations, hash.length * 8);
      SecretKeyFactory skf = SecretKeyFactory.getInstance(g_sPBKDF2_ALGORITHM);
      byte[] testHash = skf.generateSecret(spec).getEncoded();
      
      int diff = hash.length ^ testHash.length;
      for(int i = 0; i < hash.length && i < testHash.length; i++)
      {
        diff |= hash[i] ^ testHash[i];
      }
      bReturn = diff == 0;
    }
    catch (NoSuchAlgorithmException | InvalidKeySpecException ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
    }
    return bReturn;
  }
  
  public static String hash(File oFile)
  {
    String hashhex = "";
    FileInputStream oFileInputStream = null;
    try 
    {
      // public  byte [] hash(MessageDigest digest, BufferedInputStream in, int bufferSize) throws IOException {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      
      int sizeRead;
      oFileInputStream = new FileInputStream(oFile);
      try (BufferedInputStream oBufferedInputStream = new BufferedInputStream(oFileInputStream))
      {
        while ((sizeRead = oBufferedInputStream.read(yBUFFER)) != -1)
        {
          md.update(yBUFFER, 0, sizeRead);
          if (cConfig.instance().getHashFirstBlockOnly())
          {
            break;
          }
        }
      }

      byte[] hash = md.digest();
      hashhex = new String(Hex.encode(hash));
    }
    catch (Exception ex)
    {
      if (ex.getMessage().contains("used by another process") || ex.getMessage().contains("The process cannot access the file because another process has locked a portion of the file"))
      {
        Logger.getLogger(cCryptographer.class.getName()).log(Level.WARNING, "Cannot access file, it is in use: ''{0}''", oFile.getAbsolutePath());
      }
      else if (ex.getMessage().contains("Access is denied"))
      {
        Logger.getLogger(cCryptographer.class.getName()).log(Level.WARNING, "Cannot access file, Access Denied: ''{0}''", oFile.getAbsolutePath());
      }
      else if (ex.getMessage().contains("The system cannot find the file specified"))
      {}
      else
      {
        Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    finally
    {
      try
      {
        if (oFileInputStream != null)
        {
          oFileInputStream.close();
        }
      }
      catch (IOException ex)
      {
        Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return hashhex;
  }
  
  
  /**
   * By default this method will return a SHA-256 hash of the data
   * @param yData The data to hash
   * @return 
   */
  public static byte[] hash(byte[] yData)
  {
    return hash(yData, "SHA-256");
  }
  
  /**
   * Hash the data with the specified algorithm
   * @param yData The data to hash
   * @param sAlgorithm The algorithm to use to hash the data
   * @return 
   */
  public static byte[] hash(byte[] yData, String sAlgorithm)
  {
    MessageDigest md;
    byte[] digest;
    try
    {
      md = MessageDigest.getInstance(sAlgorithm);
      md.update(yData); 
      digest = md.digest();
      return digest;
    }
    catch (Exception ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }
  
  /**
   * Hash the data with the specified algorithm and provider
   * @param yData The data to hash
   * @param sAlgorithm The algorithm to use to hash the data
   * @param sProvider The provider to get the algorithm from
   * @return 
   */
  public static byte[] hash(byte[] yData, String sAlgorithm, String sProvider)
  {
    MessageDigest md;
    byte[] digest;
    try
    {
      md = MessageDigest.getInstance(sAlgorithm, sProvider);
      md.update(yData); 
      digest = md.digest();
      return digest;
    }
    catch (Exception ex)
    {
      Logger.getLogger(cCryptographer.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }
  
  public static String BytesToBase64EncodedString(byte[] yData)
  {
    return Base64.encodeBase64String(yData);
  }
  
  public static byte[] StringToBase64DecodedBytes(String sText) 
  {
    return Base64.decodeBase64(sText);
  }
  
  private static byte[] getSalt()
  {
    SecureRandom sr = new SecureRandom();
    byte[] salt = new byte[16];
    sr.nextBytes(salt);
    return salt;
  }
  
  private static byte[] fromHex(String hex) throws NoSuchAlgorithmException
  {
    return Hex.decode(hex);
  }
  
  private static String toHex(byte[] array) throws NoSuchAlgorithmException
  {
    return Hex.toHexString(array);
  }
}
