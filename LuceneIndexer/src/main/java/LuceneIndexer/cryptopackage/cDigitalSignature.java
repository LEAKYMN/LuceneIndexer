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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author Philip M. Trenwith
 */
public class cDigitalSignature
{  
  private cKeyStore m_oKeyStore;
  private static final String g_sSIGNING_ALGORITHM = "SHA256WithRSA";
  
  public cDigitalSignature(cKeyStore oKeyStore)
  {
    m_oKeyStore = oKeyStore;
    Security.addProvider(new BouncyCastleProvider());
  }

  public String sign(String sData, String sKeyName)
  {
    String sSignature = "";
    
    try
    {
      Signature oSignature = Signature.getInstance(g_sSIGNING_ALGORITHM);
      
      PrivateKey oPrivateKey = m_oKeyStore.getPrivateKey(sKeyName);
      oSignature.initSign(oPrivateKey);

      cCryptographer oCrypto = new cCryptographer();
      byte[] yHash = oCrypto.hash(sData.getBytes());
      oSignature.update(yHash);
      byte[] ySignature = oSignature.sign();

      sSignature = new String(Base64.encode(ySignature));
    }
    catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex)
    {
      Logger.getLogger(cDigitalSignature.class.getName()).log(Level.SEVERE, null, ex);
    }

    return sSignature;
  }

  public boolean verify(String sData, String sSignature, Object oPublicKey)
  {
    boolean bReturn = false;
    if (oPublicKey != null)
    {
      try
      {
        Signature oSignature = Signature.getInstance(g_sSIGNING_ALGORITHM);
        oSignature.initVerify((PublicKey)oPublicKey);
        
        cCryptographer oCrypto = new cCryptographer();
        byte[] yHash = oCrypto.hash(sData.getBytes());
        oSignature.update(yHash);
        
        byte[] ySignature = Base64.decode(sSignature);
        bReturn = oSignature.verify(ySignature);
      }
      catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex)
      {
        Logger.getLogger(cDigitalSignature.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return bReturn;
  }
}
