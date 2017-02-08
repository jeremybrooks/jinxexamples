/*
The MIT License (MIT)

Copyright (c) 2014 Jeremy Brooks

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package net.jeremybrooks.jinxexample;

import net.jeremybrooks.jinx.Jinx;
import net.jeremybrooks.jinx.JinxConstants;
import net.jeremybrooks.jinx.OAuthAccessToken;
import net.jeremybrooks.jinx.api.PhotosApi;
import net.jeremybrooks.jinx.response.photos.Photo;
import net.jeremybrooks.jinx.response.photos.Photos;
import org.scribe.model.Token;

import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.EnumSet;

/**
 * Example program showing how to use Jinx to authorize a user for your application, and get a list of recent photos.
 *
 * You must have a valid Flickr API key, which you can get here:
 * <a href="https://www.flickr.com/services/apps/create/apply/">href="https://www.flickr.com/services/apps/create/apply/</a>
 *
 * @author Jeremy Brooks
 */
public class Main {

  /* Your Flickr API key */
  private static final String FLICKR_API_KEY = "YOUR_API_KEY";

  /* Your Flickr API secret */
  private static final String FLICKR_API_SECRET = "YOUR_API_SECRET";

  /* This is where the authorization token will be saved */
  private static final String AUTH_TOKEN_FILE = System.getProperty("user.home") + "/app_auth_token";

  /* Instance of jinx - required when creating instance of the API classes */
  private Jinx jinx;

  public static void main(String... args) throws Exception {
    new Main().run();
  }

  /*
   * Attempt to load a previously cached auth token. If the token cannot be loaded,
   * the user will be prompted to authorize the application.
   *
   * Once authorization succeeds, an instance of PhotosApi is created, and a list of recent photos
   * is retrieved. The photo ID and title are printed out.
   */
  private void run() {
    try {
      this.loadAuthToken();
    } catch (Exception e) {
      System.out.println("Could not load auth token, requesting authorization...");
      try {
        this.authorize();
      } catch (Exception e2) {
        System.out.println("Authorization failed.");
        e2.printStackTrace();
        System.exit(1);
      }
    }

    // authorization done, so do something...
    PhotosApi photosApi = new PhotosApi(jinx);
    try {
      // get recent photos, along with some extra info
      Photos photos = photosApi.getRecent(EnumSet.of(JinxConstants.PhotoExtras.tags, JinxConstants.PhotoExtras.date_taken),
          10, 1);
      System.out.println("Got " + photos.getPhotoList().size() + " recent photos!");
      for (Photo p : photos.getPhotoList()) {
        System.out.println("Id " + p.getPhotoId() +
            ": \"" + p.getTitle() + "\", taken on " + p.getDateTaken() +
            " with tags [" + p.getTags() + "] @ " +
            "https://www.flickr.com/photos/" + p.getOwner() + "/" + p.getPhotoId());
      }
    } catch (Exception e) {
      System.out.println("Error getting recent photos.");
      e.printStackTrace();
    }
  }


  /*
   * Walk the user through authorizing the application.
   * Once the authorization is successful, the auth token is saved so it can be used the next time.
   */
  private void authorize() throws Exception {
    jinx = new Jinx(FLICKR_API_KEY, FLICKR_API_SECRET);
    Token requestToken = jinx.getRequestToken();
    String authUrl = jinx.getAuthorizationUrl(requestToken, JinxConstants.OAuthPermissions.read);

    // direct the user to the authUrl, and prompt them to enter the validation code
    int response = JOptionPane.showConfirmDialog(
        null,
        "Click OK to open Flickr in your browser and authorize access.\n" +
            "Then come back here and enter the authorization code.",
        "Flickr Authorization Required",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.QUESTION_MESSAGE
    );
    if (response == JOptionPane.OK_OPTION) {
      Desktop.getDesktop().browse(new URI(authUrl));
      String verificationCode = JOptionPane.showInputDialog("Authorize at \n " + authUrl +
          "\nand then enter the validation code.");
      OAuthAccessToken token = jinx.getAccessToken(requestToken, verificationCode);
      if (token == null) {
        JOptionPane.showMessageDialog(
            null,
            "Authorization token was null. Flickr authorization failed.",
            "Authorization Failed",
            JOptionPane.ERROR_MESSAGE);
      } else {
        // tell jinx about the access token
        jinx.setoAuthAccessToken(token);

        // save the token for use in the future
        File f = new File(AUTH_TOKEN_FILE);
        token.store(new FileOutputStream(f));
      }
    } else {
      JOptionPane.showMessageDialog(null,
          "Flickr authorization cancelled.",
          "Cannot Proceed",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }


  /*
   * Attempt to load a previously saved auth token
   */
  private void loadAuthToken() throws Exception {
    File file = new File(AUTH_TOKEN_FILE);
    OAuthAccessToken oAuthAccessToken = new OAuthAccessToken();
    oAuthAccessToken.load(new FileInputStream(file));
    jinx = new Jinx(FLICKR_API_KEY, FLICKR_API_SECRET, oAuthAccessToken);
  }
}
