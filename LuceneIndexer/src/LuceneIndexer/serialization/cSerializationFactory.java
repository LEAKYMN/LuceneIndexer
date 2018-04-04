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
package LuceneIndexer.serialization;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Philip M. Trenwith
 */
public class cSerializationFactory
{
  private static final String g_sAlgorithm = "AES";
  
  private byte[] getKey()
  {
    return null;
  }
  
  /**
   * Serialize the object to byte array
   * @param oObj the Serializable object to serialize
   * @param bIsEncrypted true if the serialized object should be encrypted
   * @return the serialized data in byte[] format
   */
  public byte[] serialize(Serializable oObj, boolean bIsEncrypted)
  {
    byte[] yReturn = null;
    ObjectOutputStream oOutputStream = null;
    try
    {
      ByteArrayOutputStream oByteArrayOutputStream = new ByteArrayOutputStream();
      if (bIsEncrypted)
      {
        // if bIsEncrypted -> get the key from the keystore, ask the user for a password to access the keystore.
        byte[] oKey = getKey();
        SecretKeySpec oKeySpec = new SecretKeySpec(oKey, g_sAlgorithm);

        Cipher oCipher = Cipher.getInstance(g_sAlgorithm);
        oCipher.init(Cipher.ENCRYPT_MODE, oKeySpec);
        SealedObject oSealedObject = new SealedObject(oObj, oCipher);

        CipherOutputStream oCipherOutputStream = new CipherOutputStream(oByteArrayOutputStream, oCipher);
        oOutputStream = new ObjectOutputStream(oCipherOutputStream);
        oOutputStream.writeObject(oSealedObject);
      }
      else
      {
        oOutputStream = new ObjectOutputStream(oByteArrayOutputStream);
        oOutputStream.writeObject(oObj);
      }
      
      oOutputStream.flush();
      yReturn = oByteArrayOutputStream.toByteArray();
    }
    catch (InvalidKeyException | NoSuchAlgorithmException | 
            NoSuchPaddingException | IOException | IllegalBlockSizeException ex)
    {
      Logger.getLogger(cSerializationFactory.class.getName()).log(Level.SEVERE, null, ex);
    }  
    finally
    {
      if (oOutputStream != null)
      {
        try
        {
          oOutputStream.close();
        }
        catch (IOException ex)
        {
          Logger.getLogger(cSerializationFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    return yReturn;
  }
  
  /**
   * Serialize the object to file
   * @param oObj the Serializable object to serialize
   * @param oOutputFile The file that should be used as output
   * @param bIsEncrypted true if the serialized object should be encrypted
   * @return true if the data was serialized successfully
   */
  public boolean serialize(Serializable oObj, File oOutputFile, boolean bIsEncrypted)
  {
    boolean bReturn = false;
    ObjectOutputStream oOutputStream = null;
    try
    {
      oOutputFile.getParentFile().mkdirs();
      FileOutputStream oFileOutputStream = new FileOutputStream(oOutputFile);    
      BufferedOutputStream oBufferedOutputStream = new BufferedOutputStream(oFileOutputStream);
      
      if (bIsEncrypted)
      {
        // if bIsEncrypted -> get the key from the keystore, ask the user for a password to access the keystore.
        byte[] oKey = getKey();
        SecretKeySpec oKeySpec = new SecretKeySpec(oKey, g_sAlgorithm);

        Cipher oCipher = Cipher.getInstance(g_sAlgorithm);
        oCipher.init(Cipher.ENCRYPT_MODE, oKeySpec);
        SealedObject oSealedObject = new SealedObject(oObj, oCipher);

        CipherOutputStream oCipherOutputStream = new CipherOutputStream(oBufferedOutputStream, oCipher);
        oOutputStream = new ObjectOutputStream(oCipherOutputStream);
        oOutputStream.writeObject(oSealedObject);
      }
      else
      {
        oOutputStream = new ObjectOutputStream(oBufferedOutputStream);
        oOutputStream.writeObject(oObj);
      }
      
      oOutputStream.flush();
      bReturn = true;
    }
    catch (InvalidKeyException | NoSuchAlgorithmException | 
            NoSuchPaddingException | IOException | IllegalBlockSizeException ex)
    {
      Logger.getLogger(cSerializationFactory.class.getName()).log(Level.SEVERE, null, ex);
    }    
    finally
    {
      if (oOutputStream != null)
      {
        try
        {
          oOutputStream.close();
        }
        catch (IOException ex)
        {
          bReturn = false;
          Logger.getLogger(cSerializationFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    return bReturn;
  }
  
  /**
   * Deserialize the object from file
   * @param oFile the file on disc that contains the serialized data
   * @param bIsEncrypted true if the serialized object is encrypted
   * @return the deserialized object
   */
  public Serializable deserialize(File oFile, boolean bIsEncrypted)
  {
    Serializable oReturn = null;
    try
    {
      FileInputStream oFileInputStreamout = new FileInputStream(oFile);    
      BufferedInputStream oBufferedInputStream = new BufferedInputStream(oFileInputStreamout);
      
      if (bIsEncrypted)
      {
        // if bIsEncrypted -> get the key from the keystore, ask the user for a password to access the keystore.
        byte[] oKey = getKey();
        SecretKeySpec oKeySpec = new SecretKeySpec(oKey, g_sAlgorithm);
        Cipher oCipher = Cipher.getInstance(g_sAlgorithm);
        oCipher.init(Cipher.DECRYPT_MODE, oKeySpec);

        CipherInputStream oCipherInputStream = new CipherInputStream(oBufferedInputStream, oCipher);
        ObjectInputStream oInputStream = new ObjectInputStream(oCipherInputStream);
        SealedObject oSealedObject = (SealedObject) oInputStream.readObject();
        oReturn = (Serializable)oSealedObject.getObject(oCipher);
      }
      else
      {
        try (ObjectInputStream oInputStream = new ObjectInputStream(oBufferedInputStream))
        {
          oReturn = (Serializable)oInputStream.readObject();
        }
      }
    }
    catch (NoSuchAlgorithmException | NoSuchPaddingException | 
            InvalidKeyException | IOException | ClassNotFoundException | 
            IllegalBlockSizeException | BadPaddingException ex)
    {
      Logger.getLogger(cSerializationFactory.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    return oReturn;
  }
  
  /**
   * Deserialize the byte array
   * @param yData the byte array that contains the serialized data
   * @param bIsEncrypted true if the serialized object is encrypted
   * @return the deserialized object
   */
  public Serializable deserialize(byte[] yData, boolean bIsEncrypted)
  {
    Serializable oReturn = null;
    try
    {
      ByteArrayInputStream oByteArrayInputStream = new ByteArrayInputStream(yData);
      if (bIsEncrypted)
      {
        // if bIsEncrypted -> get the key from the keystore, ask the user for a password to access the keystore.
        byte[] oKey = getKey();
        SecretKeySpec oKeySpec = new SecretKeySpec(oKey, g_sAlgorithm);
        Cipher oCipher = Cipher.getInstance(g_sAlgorithm);
        oCipher.init(Cipher.DECRYPT_MODE, oKeySpec);

        CipherInputStream oCipherInputStream = new CipherInputStream(oByteArrayInputStream, oCipher);
        ObjectInputStream oInputStream = new ObjectInputStream(oCipherInputStream);
        SealedObject oSealedObject;

        oSealedObject = (SealedObject) oInputStream.readObject();
        oReturn = (Serializable) oSealedObject.getObject(oCipher);
      }
      else
      {
        try (ObjectInputStream oInputStream = new ObjectInputStream(oByteArrayInputStream))
        {
          oReturn = (Serializable) oInputStream.readObject();
        }
      }
    }
    catch (NoSuchAlgorithmException | NoSuchPaddingException | 
            InvalidKeyException | IOException | ClassNotFoundException | 
            IllegalBlockSizeException | BadPaddingException ex)
    {
      Logger.getLogger(cSerializationFactory.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    return oReturn;
  }
}
