// Copyright (C) 2009 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.pgm.util;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.util.HttpSupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Get optional or required 3rd party library files into $site_path/lib. */
public class LibraryDownloader {
  private final ConsoleUI console;
  private final File libDirectory;
  private boolean required;
  private String name;
  private String jarUrl;
  private String sha1;
  private File dst;

  public LibraryDownloader(final ConsoleUI console, final File sitePath) {
    this.console = console;
    this.libDirectory = new File(sitePath, "lib");
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setJarUrl(final String url) {
    this.jarUrl = url;
  }

  public void setSHA1(final String sha1) {
    this.sha1 = sha1;
  }

  public void downloadRequired() {
    this.required = true;
    download();
  }

  public void downloadOptional() {
    this.required = false;
    download();
  }

  private void download() {
    if (jarUrl == null || !jarUrl.contains("/")) {
      throw new IllegalStateException("Invalid JarUrl for " + name);
    }

    final String jarName = jarUrl.substring(jarUrl.lastIndexOf('/') + 1);
    if (jarName.contains("/") || jarName.contains("\\")) {
      throw new IllegalStateException("Invalid JarUrl: " + jarUrl);
    }

    if (name == null) {
      name = jarName;
    }

    dst = new File(libDirectory, jarName);
    if (!dst.exists() && shouldGet()) {
      doGet();
    }
  }

  private boolean shouldGet() {
    if (console.isBatch()) {
      return required;

    } else {
      final StringBuilder msg = new StringBuilder();
      msg.append("\n");
      msg.append("Gerrit Code Review is not shipped with %s\n");
      if (required) {
        msg.append("**  This library is required for your configuration. **\n");
      } else {
        msg.append("  If available, Gerrit can take advantage of features\n");
        msg.append("  in the library, but will also function without it.\n");
      }
      msg.append("Download and install it now");
      return console.yesno(true, msg.toString(), name);
    }
  }

  private void doGet() {
    if (!libDirectory.exists() && !libDirectory.mkdirs()) {
      throw new Die("Cannot create " + libDirectory);
    }

    try {
      doGetByHttp();
      verifyFileChecksum();
    } catch (IOException err) {
      dst.delete();

      if (console.isBatch()) {
        throw new Die("error: Cannot get " + jarUrl, err);
      }

      System.err.println();
      System.err.println();
      System.err.println("error: " + err.getMessage());
      System.err.println("Please download:");
      System.err.println();
      System.err.println("  " + jarUrl);
      System.err.println();
      System.err.println("and save as:");
      System.err.println();
      System.err.println("  " + dst.getAbsolutePath());
      System.err.println();
      System.err.flush();

      console.waitForUser();

      if (dst.exists()) {
        verifyFileChecksum();

      } else if (!console.yesno(!required, "Continue without this library")) {
        throw new Die("aborted by user");
      }
    }
  }

  private void doGetByHttp() throws IOException {
    System.err.print("Downloading " + jarUrl + " ...");
    System.err.flush();
    try {
      final ProxySelector proxySelector = ProxySelector.getDefault();
      final URL url = new URL(jarUrl);
      final Proxy proxy = HttpSupport.proxyFor(proxySelector, url);
      final HttpURLConnection c = (HttpURLConnection) url.openConnection(proxy);
      final InputStream in;

      switch (HttpSupport.response(c)) {
        case HttpURLConnection.HTTP_OK:
          in = c.getInputStream();
          break;

        case HttpURLConnection.HTTP_NOT_FOUND:
          throw new FileNotFoundException(url.toString());

        default:
          throw new IOException(url.toString() + ": " + HttpSupport.response(c)
              + " " + c.getResponseMessage());
      }

      try {
        final OutputStream out = new FileOutputStream(dst);
        try {
          final byte[] buf = new byte[8192];
          int n;
          while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
          }
        } finally {
          out.close();
        }
      } finally {
        in.close();
      }
      System.err.println(" OK");
      System.err.flush();
    } catch (IOException err) {
      dst.delete();
      System.err.println(" !! FAIL !!");
      System.err.flush();
      throw err;
    }
  }

  private void verifyFileChecksum() {
    if (sha1 != null) {
      try {
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        final FileInputStream in = new FileInputStream(dst);
        try {
          final byte[] buf = new byte[8192];
          int n;
          while ((n = in.read(buf)) > 0) {
            md.update(buf, 0, n);
          }
        } finally {
          in.close();
        }

        if (sha1.equals(ObjectId.fromRaw(md.digest()).name())) {
          System.err.println("Checksum " + dst.getName() + " OK");
          System.err.flush();

        } else if (console.isBatch()) {
          dst.delete();
          throw new Die(dst + " SHA-1 checksum does not match");

        } else if (!console.yesno(null /* force an answer */,
            "error: SHA-1 checksum does not match\n" + "Use %s anyway",//
            dst.getName())) {
          dst.delete();
          throw new Die("aborted by user");
        }

      } catch (IOException checksumError) {
        dst.delete();
        throw new Die("cannot checksum " + dst, checksumError);

      } catch (NoSuchAlgorithmException checksumError) {
        dst.delete();
        throw new Die("cannot checksum " + dst, checksumError);
      }
    }
  }
}
