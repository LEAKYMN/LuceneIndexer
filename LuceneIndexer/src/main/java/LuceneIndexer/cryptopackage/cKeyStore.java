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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Philip M. Trenwith
 */
public class cKeyStore 
{ 
  private static final String g_sASYMMETRIC_KEYSTORE_FORMAT = "pkcs12";
  private static final String g_sSYMMETRIC_KEYSTORE_FORMAT  = "JCEKS";
  private final File m_oKeyStoreFile;
  private KeyStore m_oKeyStore;
  private FileInputStream m_oInputStream = null;
  private final char[] m_yPassword; 
  
  /**
   * Create a keystore
   * @param oKeyStoreFile The keystore file
   * @param yPassword the password used to secure the password
   * @param bSymmetricKeyStore will this store be used to store symmetric keys (AES) or asymmetric keys (RSA)
   */
  public cKeyStore(File oKeyStoreFile, char[] yPassword, boolean bSymmetricKeyStore)
  {
    m_oKeyStoreFile = oKeyStoreFile;
    m_yPassword = yPassword;
    try
    {
      if (bSymmetricKeyStore)
      {
        m_oKeyStore = KeyStore.getInstance(g_sSYMMETRIC_KEYSTORE_FORMAT);
      }
      else
      {
        m_oKeyStore = KeyStore.getInstance(g_sASYMMETRIC_KEYSTORE_FORMAT);
      }
      
      if (oKeyStoreFile.exists())
      {
        m_oInputStream = new FileInputStream(m_oKeyStoreFile.getAbsolutePath());
        m_oKeyStore.load(m_oInputStream, m_yPassword);
      }
      else
      {
        m_oKeyStore.load(null, m_yPassword);
      }
    }
    catch (KeyStoreException | IOException | NoSuchAlgorithmException
            | CertificateException ex)
    {
      Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public void generateKeyPair(String sKeyName)
  {
    PrivateKey oPrivateKey;
    FileOutputStream oOutputStream = null;
    try 
    {
      cCryptographer oCrypto = new cCryptographer();
      KeyPair oKeyPair = oCrypto.generateAsymmetricKeyPair(2048);
      oPrivateKey = oKeyPair.getPrivate();
      Certificate oCertificate = oCrypto.generateCertificate(oKeyPair);
      Certificate[] lsCertificates = new Certificate[1];
      lsCertificates[0] = oCertificate;
      oOutputStream = new FileOutputStream(m_oKeyStoreFile.getAbsolutePath());
      m_oKeyStore.setKeyEntry(sKeyName, oPrivateKey, m_yPassword, lsCertificates);
      m_oKeyStore.store(oOutputStream, m_yPassword);
    }
    catch (NoSuchAlgorithmException | SecurityException | KeyStoreException | IOException | CertificateException ex)
    {
      Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
    }
    finally
    {
      if (oOutputStream != null)
      {
        try 
        {
          oOutputStream.flush();
          oOutputStream.close();
        }
        catch (IOException ex) 
        {
          Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }
  
  public void storeKeyPair(String sKeyName, KeyPair oKeyPair)
  {
    PrivateKey oPrivateKey;
    FileOutputStream oOutputStream = null;
    try 
    {
      oPrivateKey = oKeyPair.getPrivate();
      cCryptographer oCrypto = new cCryptographer();
      Certificate oCertificate = oCrypto.generateCertificate(oKeyPair);
      Certificate[] lsCertificates = new Certificate[1];
      lsCertificates[0] = oCertificate;
      oOutputStream = new FileOutputStream(m_oKeyStoreFile.getAbsolutePath());
      m_oKeyStore.setKeyEntry(sKeyName, oPrivateKey, m_yPassword, lsCertificates);
      m_oKeyStore.store(oOutputStream, m_yPassword);
    }
    catch (NoSuchAlgorithmException | SecurityException | KeyStoreException | IOException | CertificateException ex)
    {
      Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
    }
    finally
    {
      if (oOutputStream != null)
      {
        try 
        {
          oOutputStream.flush();
          oOutputStream.close();
        }
        catch (IOException ex) 
        {
          Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }
  
  public void storeKeyPair(String sKeyName, KeyPair oKeyPair, Certificate[] lsCertificates)
  {
    PrivateKey oPrivateKey;
    FileOutputStream oOutputStream = null;
    try 
    {
      oPrivateKey = oKeyPair.getPrivate();
      oOutputStream = new FileOutputStream(m_oKeyStoreFile.getAbsolutePath());
      m_oKeyStore.setKeyEntry(sKeyName, oPrivateKey, m_yPassword, lsCertificates);
      m_oKeyStore.store(oOutputStream, m_yPassword);
    }
    catch (NoSuchAlgorithmException | SecurityException | KeyStoreException | IOException | CertificateException ex)
    {
      Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
    }
    finally
    {
      if (oOutputStream != null)
      {
        try 
        {
          oOutputStream.flush();
          oOutputStream.close();
        }
        catch (IOException ex) 
        {
          Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }
  
  public PrivateKey getPrivateKey(String sKeyName)
  {
    PrivateKey oPrivateKey = null;
    try
    {
      Key key = m_oKeyStore.getKey(sKeyName, m_yPassword);
      if (key == null)
      {
        generateKeyPair(sKeyName);
        key = m_oKeyStore.getKey(sKeyName, m_yPassword);
      }
      oPrivateKey = (PrivateKey) key;
    }
    catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException ex)
    {
      Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
    }
    return oPrivateKey;
  }
  
  public PublicKey getPublicKey(String sKeyName)
  {
    PublicKey oPublicKey = null;
    try
    {
      Certificate oCertificate = m_oKeyStore.getCertificate(sKeyName);
      oPublicKey = oCertificate.getPublicKey();
    }
    catch (Exception ex)
    {
      Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
    }
    return oPublicKey;
  }
  
  public Certificate getCertificate(String sKeyName)
  {
    Certificate oCertificate = null;
    try
    {
      oCertificate = m_oKeyStore.getCertificate(sKeyName);
    }
    catch (Exception ex)
    {
      Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
    }
    return oCertificate;
  }
  
  public void storeSecretKey(String sKeyName, byte[] ySecretKey)
  {
    FileOutputStream oOutputStream = null;
    try
    {
      KeyStore.ProtectionParameter oProtectionParameter = new KeyStore.PasswordProtection(m_yPassword);
      SecretKey oSecretKey = new SecretKeySpec(ySecretKey, 0, ySecretKey.length, "AES");
      KeyStore.SecretKeyEntry oSecretKeyEntry = new KeyStore.SecretKeyEntry(oSecretKey);
      oOutputStream = new FileOutputStream(m_oKeyStoreFile.getAbsolutePath());
      m_oKeyStore.setEntry(sKeyName, oSecretKeyEntry, oProtectionParameter);
      m_oKeyStore.store(oOutputStream, m_yPassword);
    }
    catch (KeyStoreException | NoSuchAlgorithmException | CertificateException 
            | IOException ex)
    {
      Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
    }
    finally
    {
      if (oOutputStream != null)
      {
        try 
        {
          oOutputStream.flush();
          oOutputStream.close();
        }
        catch (IOException ex) 
        {
          Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  public byte[] getSecretKey(String sKeyName)
  {
    byte[] yAESKey = null;
    try
    {
      KeyStore.ProtectionParameter oProtectionParameter = new KeyStore.PasswordProtection(m_yPassword);
      KeyStore.SecretKeyEntry oSecretKeyEntry = (SecretKeyEntry)m_oKeyStore.getEntry(sKeyName, oProtectionParameter);
      SecretKey oSecretKey = oSecretKeyEntry.getSecretKey();
      yAESKey = oSecretKey.getEncoded();
    }
    catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex)
    {
      Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
    }
    return yAESKey;
  }
  
  public void close()
  {
    if (m_oInputStream != null)
    {
      try
      {
        m_oInputStream.close();
      }
      catch (IOException ex)
      {
        Logger.getLogger(cKeyStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}
