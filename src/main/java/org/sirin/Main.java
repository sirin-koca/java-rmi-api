
/* Dette er mitt aller første forsøk på
å lage et veldig enkelt Java RMI-prosjekt i faget Distributed Systems (IN5020).

RMI betyr "Remote Method Invocation". Det høres avansert ut,
men det er egentlig bare at en klient kan kalle på en metode
som ligger på en server, nesten som om den var lokal.
Java fikser nettverks-kommunikasjonen i bakgrunnen.
*/

// Dette er mitt pakkenavn der filene skal ligge
package org.sirin;
public class Main {
    public static void main(String[] args) {
        // Tradisjonstro starter med å skrive ut "Hello RMI World!".
        System.out.println("Hello RMI World!");

        // Basics - variabler og datatyper i Java:

        // Et heltall (int = integer)
        int tall = 5;
        System.out.println("Tallet er: " + tall);

        // Et desimaltall (double)
        double pi = 3.14;
        System.out.println("Verdien av pi er: " + pi);

        // En tekststreng (String)
        String navn = "Sirin";
        System.out.println("Hei, jeg heter " + navn);

        // En sann/usann-verdi (boolean)
        boolean erStudent = true;
        System.out.println("Er jeg master student? " + erStudent);
    }
}