java -cp .;../../lib/okapi-lib.jar;example01.jar example01.Main myFile.html -s pseudo myFile.out-pseudo.html
java -cp .;../../lib/okapi-lib.jar;example01.jar example01.Main myFile.html -s upper myFile.out-upper.html
java -cp .;../../lib/okapi-lib.jar;example01.jar example01.Main myFile.html -s pseudo -s upper myFile.out-both.html
java -cp .;../../lib/okapi-lib.jar;example01.jar example01.Main myFile.odt -s pseudo
java -cp .;../../lib/okapi-lib.jar;example01.jar example01.Main myFile.properties -s pseudo
java -cp .;../../lib/okapi-lib.jar;example01.jar example01.Main myFile.xml -s pseudo

java -cp .;../../lib/okapi-lib.jar;example02.jar example02.Main myFile.odt

java -cp .;../../lib/okapi-lib.jar;example03.jar example03.Main

java -cp .;../../lib/okapi-lib.jar;example04.jar example04.Main

java -cp .;../../lib/okapi-lib.jar;example05.jar;../../lib/axis.jar;../../lib/wsdl4j-1.5.1.jar;../../lib/jaxrpc.jar;../../lib/commons-logging-1.0.4.jar;../../lib/commons-discovery-0.2.jar;../../lib/saaj.jar;../../lib/xmlrpc-client-3.1.jar;../../lib/xmlrpc-common-3.1.jar;../../lib/ws-commons-util-1.0.2.jar example05.Main

pause
