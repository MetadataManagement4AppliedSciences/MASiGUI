# MASi Generic Graphical User Interface

Provided are an installation guide to install the Liferay Portal and a guide to build and deploy the search interface portlet. Furthermore, an underlying installation of the KIT Datamanager is required.

## How to install and configure the KIT Datamanager
 * The installation manual is provided [here](http://datamanager.kit.edu/dama/manual/index.html)

## How to install and configure the Liferay Portal

* In order to install and configure Liferay you'll need:
	* Java SE Development Kit 8
	* PostgreSQL 9.1+
* Log into your server
* It is recommended to install Liferay/MASi with your KITDM user account. If not, you have to link `$HOME/.repoClient/` to the same location in your Liferay/MASi home directory and grant appropriate persmissions. KITDM admin account credentials should already be saved in this folder.
* Download the newest version of Liferay Community Edition major version 7.0. For example 7.0.6 GA7. Newer versions are untested:
	* https://sourceforge.net/projects/lportal/files/Liferay%20Portal/

* Unpack the file where you want to install it.
* It is recommended to create a link
```
ln -s liferaydir liferay
```
* And it is also recommended to create a generic Tomcat link:
```
cd liferaydir
ln -s tomcatdir tomcat
```
* Start the server. This can take over 2 mintes. See the catalina.out log file for the progress.
```
 liferay/tomcat/bin/startup.sh
```
* Note: The Tomcat of KIT DM and of Liferay have to have different ports.
* To be able to smoothly update to the next Liferay version, it is required to use a central database. The following steps describe the proceedure for PostgreSQL.
* Log in as root and change to the postgres user
```
su - postgres
```
* Create a user for Postgres:
```
createuser liferay
```
- Create the database:
```
createdb lportal
```
- Access the shell:
```
psql
```
- Provide the privileges to the postgres user (with new password):
```
alter user liferay with encrypted password 'newpassword';
ALTER ROLE
grant all privileges on database lportal to liferay;
GRANT
```
- log of with CTRL + D
- to test logging in:
```
psql -h localhost -U liferay lportal
```
- show all tables:
```
\d
```
- Show content of table:
```
SELECT * FROM usergroup;
```
* Access localhost:8080 and initialize Liferay.
* Portal Name: choose yours, for MASi it is: MASi Research Data Management Service
* Define the admin
* Including sample data is not required
* Configure the database:
	* Database Type: PostgreSQL
	* User Name: liferay
	* Password: somepassword
	* Then click "Finish Configuration"
* Adapt the memory limits. Example is for a server with 64 GB main memory. Add to the end of liferay/tomcat/bin/startup.sh
```
ulimit -Hn 163840
ulimit -Sn 163840
```
* And in liferay/tomcat/bin/setenv.sh:
```
CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF8 -Djava.net.preferIPv4Stack=true -Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false -Duser.timezone=GMT -Xms8g -Xmx8g -XX:PermSize=1g -XX:MaxPermSize=1g -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC"
```
* Define the KIT DM installation by setting an environment variable in liferay/tomcat/bin/start.sh before "exec "$PRGDIR"/"$EXECUTABLE" start "$@"" with your specific directory:
```
export DATAMANAGER_CONFIG=/home/kitdm/KITDM1.5/KITDM/WEB-INF/classes/datamanager.xml
```
* Add some properties to liferay/portal-ext.properties (create if not existing). Some might not be applicable to you. Restart then. See also https://docs.liferay.com/portal/7.0/propertiesdoc/portal.properties.html.
```
layout.show.portlet.access.denied=false
module.framework.properties.lpkg.index.validator.enabled=false
company.security.auth.requires.https=true

users.reminder.queries.custom.question.enabled=false
users.reminder.queries.enabled=false
users.reminder.queries.required=false

web.server.protocol=https
web.server.http.port=8443
web.server.https.port=8443
web.server.protocol=https
web.server.host=yourhost.com

mail.session.mail.pop3.host=yourprovider.com
mail.session.mail.pop3.password=yourpassword
mail.session.mail.pop3.port=995
mail.session.mail.pop3.user=user
mail.session.mail.store.protocol=pop3
mail.session.mail.smtp.host=yourprovider.com
mail.session.mail.smtp.password=yourpassword
mail.session.mail.smtp.port=587
mail.session.mail.smtp.user=user
mail.session.mail.smtp.auth=true
mail.session.mail.smtp.auth.plain.disable=false
mail.session.mail.smtp.starttls.enable=true
mail.session.mail.transport.protocol=smtp
mail.smtp.ssl.enable=true

session.timeout=3000
user.timezone=Europe/Paris
locale.prepend.friendly.url.style=0
module.framework.web.generator.generated.wabs.store=true
module.framework.web.generator.generated.wabs.store.dir=${module.framework.base.dir}/wabs
```

* Deploy Vaadin: Either download Vaadin-8.4.5 (newer versions are untested) and its dependencies (jsoup-1.11.3.jar, lib/gentyref-1.2.0.vaadin1.jar) manually or use the `build_dependencies.sh` script for automatic download to ./deploy . Copy all Vaadin jar files and its dependencies into the deploy directory of Liferay.

* To update the Vaadin libraries 
	* First, undeploy old version
	```
	rm /home/kitdm/liferay/osgi/modules/vaadin-*8.4.4*
	```
	* Then, download and deploy new version as described in the previous step

* You are able to configure that announcements are automatically send to users that did do an opt-in by adding the following lines to portal-ext.properties:
```
announcements.entry.types=alerts,news,test
announcements.entry.check.interval=1
```

* To create community area, do the following.
	* Go to Control Panel / Sites / Sites /  Add Site (plus symbol in lower right corner) /  Blank Site
		* Name: Name of Community
		* Membership Type: Restricted
		* make sure "Friendly URL" has full name instead of abbreviation
		* Save
	* Navigation / Pages / Add Public Page
		* Name: Community Area
		* Add Page
		* 1 Column
	* Navigation / Community Name / Public Pages / ... / Configure Page / Advanced
		* Click: "Merge MASi Research Data Management Service public pages"
		* Save
	* Go to Community Area Page / Navigation Menu (right upper corner: "Welcome|Community Area") / ... / Configuration
		* Display Template: Bar Default Styled
		* Save
	* Community Area / Search Field / ... / Configure
		* Set Scope to 'Everything'
		* Save
	* Go to Welcome Site / Add (small plus symbol in upper right corner) / Content / Add New / Basic Web Content
		* Name: Name of Community
		* Content: "Enter Area" mit Link zur Community Site
		* Permissions
			* Viewable by: Owner
			* More Options: remove "Add Discussion" check marks
		* Publish
	* Options of created Basic Web Content / Permissions
		* remove Guest and Site Member rights
		* add View right of role the site belongs to
		* Save
* Create groups in Liferay
	* Menu/Control Panel/Users/User Groups --> Add (large plus in lower right corner)
	* Name: same name as in KIT DM in column "GROUP ID"
	* Save
* Create roles in Liferay
	* Name: Member of ... (full community name)
	* Save
* Assign group, role and site to user
	* Control Panel / Users / Users and Organisation / Configure / Edit
	* Sites / Name: site name
	* User Groups / Name: group name
	* Roles / Title: role name
	* Save
* Assign groups to respective sites
	* Control Panel --> Sites --> Sites --> choose Site Name
	* Members --> Site Membership --> User Groups
	* Click on large "plus" symbol
	* Choose group
	* Done
* Delete log files of Liferay and KIT DM after 7 days
	* Create /root/cleanlogs.sh with the following content
	```	
	find /home/kitdm/liferay/tomcat/logs -name "*" -type f -mtime +7 -exec rm -f {} \;
	find /home/kitdm/liferay/osgi/state -name "*.log" -type f -mtime +7 -exec rm -f {} \;
	find /home/kitdm/liferay/logs -name "*" -type f -mtime +7 -exec rm -f {} \;
	find /var/log/tomcat7/ -name "*" -type f -mtime +7 -exec rm -f {} \;
	```
	* Create the following cronjob with "crontab -e":
	```
	0 * * * * /root/cleanlogs.sh
	```
* Remove unnecessary components in user area
	* Control Panel / Configuration / Components
	* deactivate:
		* My Organisation
		* My Submissions 
		* My Sync Devices 
		* My Workflow Tasks 
		* Notifications
	* Control Panel / Users / Roles / User / Define permissions / User / My Account
	* Uncheck "Access in My Account" in each of the above
* In case you need all main adresses of the Lifery users:
	* Control Panel / Users / Users and Organisatzions
	* '...' in uppermost right corner
	* Export Users
* Create test users
	* Menu/Control Panel/Users/Users and Organisations - Add (large "plus" symbol in lower right corner)
* Create and apply for certificate for HTTPS
	* Create certificate request
	```
	openssl req -new -config zih-generic-req.conf -newkey rsa:4096 -sha256 -keyout privkey.pem -outform PEM -out certreq.pem
	```
	* Apply for certificate, for example, at DFN
	* Store all CA certs of the chain in allcacerts.crt
	```
	cat /etc/ssl/certs/T-TeleSec_GlobalRoot_Class_2.pem TUDresdenCA.crt DFN-VereinCertificationAuthority2.crt > allcacerts.crt
	```
	* Convert private key and cert reply to P12 format including all ca certs
	```
	openssl pkcs12 -export -out masi.p12 -inkey privkey.pem -in cert-9137923098622209082834211991.pem -chain -CAfile allcacerts.crt -passout pass:password
	```
	* Check P12
	```
	openssl pkcs12 -info -in keystore.p12
	```
	* Convert to JKS format
	```
	keytool -importkeystore -srckeystore masi.p12 -srcstoretype PKCS12 -srcstorepass password -destkeystore masi.jks  -deststorepass password
	```
	* Check JKS
	```
	keytool -list -v -keystore masi.jks -storepass password
	```
* Configure HTTPS in Liferay
	* In liferay/tomcat/conf/server.xml include the following after the "org.apache.coyote.http11.Http11NioProtocol" section. The ciphers are adapted from https://wiki.mozilla.org/Security/Server_Side_TLS --> yellow (1.12.17)
	```
         <Connector 
                port="8443" 
                protocol="org.apache.coyote.http11.Http11NioProtocol" 
                maxThreads="150" 
                SSLEnabled="true" 
                scheme="https" 
                secure="true" 
                clientAuth="false" 
                sslProtocol="TLS"
                sslEnabledProtocols="TLSv1.2,TLSv1.1,TLSv1"  
                ciphers=" TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 , TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256 ,  TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 ,  TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384 ,  TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 ,  TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 ,  TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384 , TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384 , TLS_DHE_RSA_WITH_AES_128_GCM_SHA256 ,  TLS_DHE_RSA_WITH_AES_256_GCM_SHA384 ,  TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA ,  TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA ,  TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA ,  TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA ,  TLS_DHE_RSA_WITH_AES_128_CBC_SHA256 ,  TLS_DHE_RSA_WITH_AES_128_CBC_SHA ,  TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 ,  TLS_DHE_RSA_WITH_AES_256_CBC_SHA ,  TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA ,  TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA ,  TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA ,  TLS_RSA_WITH_AES_128_GCM_SHA256 ,  TLS_RSA_WITH_AES_256_GCM_SHA384 ,  TLS_RSA_WITH_AES_128_CBC_SHA256 ,  TLS_RSA_WITH_AES_256_CBC_SHA256 , TLS_RSA_WITH_AES_128_CBC_SHA ,  TLS_RSA_WITH_AES_256_CBC_SHA , TLS_RSA_WITH_3DES_EDE_CBC_SHA"
                keystoreFile="/home/kitdm/certificate/masi.jks" 
                keyAlias="1" 
                keystorePass="MASiMASi" 
                proxyPort="443"
                proxyName="masi.zih.tu-dresden.de"
         />
	```
	* Change in liferay/tomcat/conf/server.xml from
	```
	    <Connector port="8081" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" URIEncoding="UTF-8" />
	```
		* to the following
	```
  	  <Connector port="8081" protocol="HTTP/1.1"
      	         connectionTimeout="20000"
       	        redirectPort="443" URIEncoding="UTF-8" />
	```
	* Include in conf/web.xml on the bottom before <\web-app>
	```
	<security-constraint>
	<web-resource-collection>
	<web-resource-name>Protected Context</web-resource-name>
	<url-pattern>/*</url-pattern>
	</web-resource-collection>
	<!-- auth-constraint goes here if you requre authentication -->
	<user-data-constraint>
	<transport-guarantee>CONFIDENTIAL</transport-guarantee>
	</user-data-constraint>
	</security-constraint>
	```
* Configure HTTPS in KIT DM
	* In /etc/tomcat7/server.xml include (instead of the previous "Connector port="9090""  connector) (ciphers from https://wiki.mozilla.org/Security/Server_Side_TLS --> yellow (1.12.17):
	```
         <Connector 
                port="9090" 
                protocol="org.apache.coyote.http11.Http11NioProtocol" 
                maxThreads="150" 
                SSLEnabled="true" 
                scheme="https" 
                secure="true" 
                clientAuth="false" 
                sslProtocol="TLS"
                sslEnabledProtocols="TLSv1.2,TLSv1.1,TLSv1"  
                ciphers=" TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 , TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256 ,  TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 ,  TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384 ,  TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 ,  TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 ,  TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384 , TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384 , TLS_DHE_RSA_WITH_AES_128_GCM_SHA256 ,  TLS_DHE_RSA_WITH_AES_256_GCM_SHA384 ,  TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA ,  TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA ,  TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA ,  TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA ,  TLS_DHE_RSA_WITH_AES_128_CBC_SHA256 ,  TLS_DHE_RSA_WITH_AES_128_CBC_SHA ,  TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 ,  TLS_DHE_RSA_WITH_AES_256_CBC_SHA ,  TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA ,  TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA ,  TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA ,  TLS_RSA_WITH_AES_128_GCM_SHA256 ,  TLS_RSA_WITH_AES_256_GCM_SHA384 ,  TLS_RSA_WITH_AES_128_CBC_SHA256 ,  TLS_RSA_WITH_AES_256_CBC_SHA256 , TLS_RSA_WITH_AES_128_CBC_SHA ,  TLS_RSA_WITH_AES_256_CBC_SHA , TLS_RSA_WITH_3DES_EDE_CBC_SHA"
                keystoreFile="/home/kitdm/certificate/masi.jks" 
                keystorePass="MASiMASi" 
         />
	```
	* Make sure to change all links/URLs from HTTP to HTTPS in your KIT DM config file: /home/kitdm/datamanager/KITDM/WEB-INF/classes/datamanager.xml
* Adapt the name in the admin user interface of KIT DM
	* In /home/kitdm/datamanager/KITDM/WEB-INF/classes/datamanager.xml change the "repositoryName" parameter to your chosen name
* Configure the fowarding for HTTPS (you might need to adapt it to your situation)
```
iptables -t nat -F
iptables -t nat -A OUTPUT -d localhost -p tcp --dport 443 -j REDIRECT --to-ports 8443
iptables -t nat -A OUTPUT -d $HOSTNAME -p tcp --dport 443 -j REDIRECT --to-ports 8443
iptables -t nat -A PREROUTING -d $HOSTNAME -p tcp --dport 443 -j REDIRECT --to-ports 8443
iptables -t nat -A OUTPUT -d localhost -p tcp --dport 80 -j REDIRECT --to-ports 8081
iptables -t nat -A OUTPUT -d $HOSTNAME -p tcp --dport 80 -j REDIRECT --to-ports 8081
iptables -t nat -A PREROUTING -d $HOSTNAME -p tcp --dport 80 -j REDIRECT --to-ports 8081
iptables-save > /etc/iptables/rules.v4
```
* Install APR libs for Tomcat to get rid of this message:
	```
	INFO [main] org.apache.catalina.core.AprLifecycleListener.lifecycleEvent The APR based Apache Tomcat Native library which allows optimal performance in production environments was not found on the java.library.path: /usr/java/packages/lib/amd64:/usr/lib/x86_64-linux-gnu/jni:/lib/x86_64-linux-gnu:/usr/lib/x86_64-linux-gnu:/usr/lib/jni:/lib:/usr/lib
	```
	```
	sudo apt-get install libtcnative-1 libssl-dev libapr1-dev libapr1
	```
* Deploy the Liferay service script to /etc/init.d/ and configure it to automatically start Liferay after booting
* LDAP Connection
	* Login as administrator
	* Control Panel → Configuration → Instance Settings --> Authentication --> LDAP
	* Choose "Enable", Disable "Enable User Password on Import", Enable "Use LDAP Password Policy" then "Save"
	* LDAP Servers --> Add - For example:
		* Server Name: ZIH LDAP
		* Base Provider URL: ldaps://...
		* Base DN: dc=tu-dresden,dc=de
		* Principal: cn=masi,dc=masi,dc=zih,dc=tu-dresden,dc=de
		* Credentials: LDAP credential
		* Test LDAP Connection
		* Test LDAP Users
* Deploy and configure the MASi theme to disable "powered by Liferay" and enabled the logos of the partners and the DFG.
	* Deploy the theme by copying masi-theme.war file to liferay/deploy.
	* For every community site and in the site template choose the following
		* Control Panel / Sites / open every one and click on the three vertical dots right of "Public Pages" / Configure / Change Current Theme / choose masi-theme / Save
* Some further configuration stuff
	* Control Panel → Configuration → Instance Settings --> Users
		* Disable "Enable Birthday" und Disable "Enable Gender"
	* Control Panel → Configuration → Instance Settings
		* Name: MASi Research Data Management Service
		* Mail Domain: example.de
		* Virtual Host: masi.zih.tu-dresden.de
		* Default Landing Page: /
	* Control Panel → Configuration → Instance Settings --> Authentication
		* Disable "Allow users to request forgotten passwords?" and "Allow users to request password reset links?"
	* Control Panel / Configuration / Instance Settings / Miscellaneous
		* Default Language: German (Germany)
		* Available Languages: English (United States), Germany (Germany)
		* Time Zone: UTC + 1
		* Logo / Change / Select --> transparentdefaultlogo.png
		* Default Theme: masi-theme
	* Save
* Increase user sesson timeout in liferay/tomcat/webapps/ROOT/WEB-INF/web.xml
	```
	<session-timeout>300</session-timeout>
	```
* Mitigate javascript bootstrap error in liferay/tomcat/webapps/ROOT/WEB-INF/web.xml
	```
    <session-config>
        <tracking-mode>COOKIE</tracking-mode>
    </session-config>
	```
* Add the "Impressum"
	* "MASi Research Data Management Service" --> Navigation --> Public Pages --> Add Public Page
		* Name: Impressum
		* Option: "1 Colomn"
		* Activate "Hide from Navigation Menu"
		* Save
	* Add --> Content --> Add New --> Basic Web Content
		* Title: Impressum
		* Edit Web Content: click in "Content" area, then click on "</>" on the right side and include the content of Impressum.txt
* Add the "Nutzungsbedingungen"
	* "MASi Research Data Management Service" --> Navigation --> Public Pages --> Add Public Page
		* Name: Nutzungsbedingungen
		* Option: "1 Colomn"
		* Activate "Hide from Navigation Menu"
		* Save
	* Add (right upper corner) --> Content --> Add New --> Basic Web Content
		* Title: Nutzungsbedingungen
		* Edit Web Content: click in "Content" area, then click on "</>" on the right side and include the content of Nutzungsbedingungen.txt
	* Let the users accept the Nutzungsbedingungen on first login
		* Control Panel → Configuration → Instance Settings --> General --> Terms of Use
		* Terms of Use Web Content: (the number of the Basic Web Content you just created)
		* Article ID: (the name of the site that is displayed when the "Nutzungsbedingungen" are edited)
		* Save
* Add the contact site
	* "MASi Research Data Management Service" --> Navigation --> Public Pages --> Add Public Page
		* Name: Kontakt
		* Option: "1 Colomn"
		* Activate "Hide from Navigation Menu"
		* Save
	* Add --> Content --> Add New --> Basic Web Content
		* Title: Kontakt
		* Edit Web Content: click in "Content" area, then click on "</>" on the right side
			* For the German version include the content of Kontakt_ger.txt
			* For the English version include the content of Kontakt_eng.txt
* Add the project site
	* "MASi Research Data Management Service" --> Navigation --> Public Pages --> Add Public Page
		* Name: Projekt
		* Option: "1 Colomn"
		* Activate "Hide from Navigation Menu"
		* Save
	* Add --> Content --> Add New --> Basic Web Content
		* Title: Projekt
		* Edit Web Content: click in "Content" area, then click on "</>" on the right side
			* For the German version include the content of Projekt_ger.txt
			* For the English version include the content of Projekt_eng.txt
* Add the publication site
	* "MASi Research Data Management Service" --> Navigation --> Public Pages --> Add Public Page
		* Name: Publikation
		* Option: "1 Colomn"
		* Activate "Hide from Navigation Menu"
		* Save
	* Add --> Content --> Add New --> Basic Web Content
		* Title: Publikation
		* Edit Web Content: click in "Content" area, then click on "</>" on the right side
			* For the German version include the content of Publikation_ger.txt
			* For the English version include the content of Publikation_eng.txt
* To create links on the bottom for the five just mentioned sites
	* Place the file body_bottom-ext.jsp in liferay/tomcat/webapps/ROOT/html/common/themes/
	* If you have renamed something modify the file accordingly
* Sometimes there is a problem that after a restart of Liferay the Search Portal and the Vaadin Liferay Integration library are not available. A workaround is to redeploy both after each restart of Liferay. Adapt paths where required.
	```
	cp /path/to/MASiGenGUI*.jar /home/kitdm/liferay/deploy/
	cp /path/to/vaadin-liferay-integration*.jar /home/kitdm/liferay/deploy/
	```
* Add the start page.
	* Choose 2-1 layout
	* Welcome display
		* Add Web Content Display to left column
		* Name "Welcome"
		* In German version Include content of Welcome_ger.txt
		* In English version Include content of Welcome_eng.txt
	* Add news display
		* "Plus" symbol / search for "Alerts" / drag "Alerts" display above Welcome display
		* A Web Content Display for earch community site containing link to the community site for example with the text "Enter Area" / "Bereich betreten". Then adjust the permission to only allow member of that community to see the respective display.
* Add documentation page.
	* "MASi Research Data Management Service" --> Navigation --> Public Pages --> Add Public Page
		* Name: Dokumentation
		* Option: "1 Colomn"
		* Add Page
	* Add --> Content --> Add New --> Basic Web Content
	* Title: Documentation
	* Edit Web Content: click in "Content" area, then click on "</>" on the right side
		* For the German version include the content of Documentation_ger.txt
		* For the English version include the content of Documentation_eng.txt
* Mail Notification Templates
	* Control Panel → Configuration → Instance Settings --> Email Notifications --> Sender
		* Name: MASi Research Data Management Service
		* Address: masi@mailbox.tu-dresden.de
	* Control Panel → Configuration → Instance Settings --> Email Notifications --> Account Created Notification
		* include in source mode: Account Created Notification - Body with Password.txt
		* include in source mode: Account Created Notification - Body without Password.txt
	* Control Panel → Configuration → Instance Settings --> Email Notifications --> Email Verfification Notification
		* include in source mode: Email Verfification Notification.txt
	* Control Panel → Configuration → Instance Settings --> Email Notifications --> Password Changed Notification
		* include in source mode: Password Changed Notification.txt
	* Control Panel → Configuration → Instance Settings --> Email Notifications --> Password Reset Notification
		* include in source mode: Password Reset Notification.txt
	* Save
* Deactivate week ciphers and mac selection for SSH
	* Append to /etc/ssh/sshd_config
	```
	# Deactivate weak ciphers and mac selection
Ciphers aes128-ctr,aes192-ctr,aes256-ctr,aes128-gcm@openssh.com,aes256-gcm@openssh.com,chacha20-poly1305@openssh.com
MACs hmac-sha1-etm@openssh.com,umac-64-etm@openssh.com,umac-128-etm@openssh.com,hmac-sha2-256-etm@openssh.com,hmac-sha2-512-etm@openssh.com,hmac-ripemd160-etm@openssh.com,hmac-sha1,umac-64@openssh.com,umac-128@openssh.com,hmac-sha2-256,hmac-sha2-512,hmac-ripemd160
	```

* You need to create cache directories. Either you create them in `/home/cache/` as root:

```
mkdir -p /home/cache/valueCache /home/cache/resultCache /home/cache/refineCache /home/cache/refineDateCache
chown -R yourLRUser:yourLRUser /home/cache/

```
** or you change the hardcoded paths in `PaginationPanel.java` and `PaginationPanel.java`. Search for `/home/cache`.

## KITDM Access
* Create the following account in KITDM:
    * Username: guest
    * Email: default@liferay.com
* And a user group:
    * Name: Liferay
    * GroupID: Liferay
* If you want guests (without user account) to be able to search the catalogue, you need give the appropriate KITDM permissions to the guest account.
* Now you can add the guest account to all groups you want to be accessable publicly.
* In Liferay you need to enable access to the search frontpage for guests or restrict it for specific site members:
    * Left Navigation / Liferay / Navigation / Welcome / hover over search / ... / Permissions
* If you don't want to use guest access, please remove permissions for guests and remove permissions for liferay user in KITDM. 
* Guest user permissions and KITDM permissions should match. 

## How to build and deploy the Search Interface Portlet

* In order to build the MASiGenGUI you'll need:
	* Java SE Development Kit 8
	* Apache Maven 3

* To fetch, build and install KITDM-base 1.5 and KITDM-repoclient 1.5.1 to your maven repository, run this script:
```
user@localhost:/home/user/MASiGenGUI/$ ./build_dependencies.sh
```

* Change your datamanager database credentials (not liferay!) in src/main/resources/META-INF/persistence.xml at every occurence of `javax.persistence.jdbc`.

* And to build the portlet:

```
user@localhost:/home/user/MASiGenGUI/$ mvn clean package
...

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 01:13 min
[INFO] Finished at: 2019-03-04T10:17:34+01:00
[INFO] Final Memory: 57M/956M
[INFO] ------------------------------------------------------------------------

```
* As soon as the assembly process is finished there will be a jar file located at /home/user/MASiGenGUI/target. Deploy it by copying the jar file to the `deploy/` directory of your Liferay installation.

* Now you can login to you liferay instance and click the `+` button in the upper right corner, to add MASi. At `Applications` search for `MASi`.

* For production use, you should consider configuring a external Elasticsearch daemon in Liferay settings.

## Troubleshooting 

* To see if all modules were loaded correctly you can use the felix gogo shell. It provies access the OSGI environment:
	* On server: `telnet localhost 11311`
	* list bundles: lb
	* start / stop
	* Further commands: https://dev.liferay.com/develop/reference/-/knowledge_base/7-0/using-the-felix-gogo-shell

* If you encounter the following error, you can try to delete `vaadin-liferay-integration*.jar` from `liferay/osgi/modules` and redeploy it.
```
[com_vaadin_liferay_integration:97] [com.vaadin.osgi.liferay.VaadinPortletProvider(2532)] The activate method has thrown an exception
 com.vaadin.osgi.resources.OsgiVaadinResources$ResourceBundleInactiveException: Vaadin Shared is not active!
```
* You might need to repeat the procedure every time you deploy a new MASiGUI version. 
* This bug might not be visible in felix gogo shell.
* This seems to be a bug in vaadin: https://github.com/vaadin/framework/issues/10220

* If KITDM (ingest) is not working, check pgsql privileges on datamanager table.

## More Information

* [MASi Service](https://masi.zih.tu-dresden.de)

## License

The MASiGenGUI is licensed under the Apache License, Version 2.0.


