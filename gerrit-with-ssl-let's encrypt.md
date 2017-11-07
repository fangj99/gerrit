# Gerrit work with Self-Signed SSL

## Setup gerrit.config file
```
[gerrit]
        basePath = git
        canonicalWebUrl = https://172.16.99.212/
......
[httpd]
        listenUrl = proxy-https://127.0.0.1:8081/
```


## Using openssl create key, csr and crt
```
openssl genrsa -out /path/to/www_server_com.key 4096
openssl req -new -key /path/to/www_server_com.key -out /path/to/www_server_com.csr

openssl x509 \
       -signkey www_server_com.key \
       -in www_server_com.csr \
       -req -days 365 -out www_server_com.crt
```


## Setup apache with ssl support and restart apache2 after setup

### Enable apache2's ssl support
```
a2enmod ssl
```
### Add created crt and key to the apache2 conf file for gerrit(gerrit proxy port is 8012, yours maybe different) 
#### The following line need to be http, not https: "ProxyPass / http://127.0.0.1:8081/ nocanon"
```
<VirtualHost *:8012>
  SSLEngine on
  SSLCertificateFile    /etc/httpd/conf/www_server_com.crt
  SSLCertificateKeyFile /etc/httpd/conf/www_server_com.key

  ServerName xxx.xxx.xxx.xxx

  ProxyRequests Off
  ProxyVia Off
  ProxyPreserveHost On

  <Proxy *>
    Order deny,allow
    Allow from all
  </Proxy>

  AllowEncodedSlashes On
  ProxyPass / http://127.0.0.1:8081/ nocanon  #Here need to be http, not https
</VirtualHost>
```


# Gerrit work with Let's Encrypt SSL
## Created the Let's Encrypt key and crt files by follow the steps in the link below
https://github.com/fangj99/acme-tiny/blob/master/apache.md

## Change the apache2 conf file to open 80 port for Let's Encrypt to verfity and later renewal
```
<VirtualHost *:80>
   ServerName www.yoursite.com
   ServerAlias yoursite.com

   Alias /.well-known/acme-challenge/ /home/xxxxxxx/public_html/challenges/
   <Directory /home/xxxxxxx/public_html/challenges/>
      AllowOverride None
      Require all granted
      Satisfy Any
   </Directory>

   # rest of your config for this server
   # DocumentRoot, ErrorLog, CustomLog...
</VirtualHost>

```
## Under the gerrit virtualhost settings(my gerrit proxy port is 8012), insert the SSL settings of the two files(signed.crt, domain.key) from Let's Encrypt
```
<VirtualHost *:8012>


  SSLEngine on
  SSLCertificateFile    /usr/local/etc/apache/keys/gerrit/signed.crt
  SSLCertificateKeyFile /usr/local/etc/apache/keys/gerrit/domain.key

   
```
