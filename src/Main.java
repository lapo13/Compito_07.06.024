import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static java.lang.Thread.sleep;
class threadGenerator extends Thread {
    private inputQueue uniqueQueue;
    private int threadID;
    private int val;
    private int X;
    private int genCount = 0;
    private int N; // non presente nel codice consegnato

    public threadGenerator(inputQueue uniqueQueue, int threadID, int sleepTime, int N) {
        this.uniqueQueue = uniqueQueue;
        this.threadID = threadID;
        this.val = this.threadID;
        this.X = sleepTime;
        this.N = N;
    }

    @Override
    public void run() {
        try{
            while(true) {
                val++;
                if(uniqueQueue.add(val)==-1){
                    throw new InterruptedException();
                }
                genCount++;
                sleep(X);
            }
        }catch (InterruptedException e) {
            if(threadID == 0) {// blocco if inserito per pulire l'output dato che i messaggi generati sono gli stessi per come sono stati implementati i generatori

                System.out.println("Total generated values: " + genCount * N + " values."); // nel compito consegnato il totale
                // è stato messo erroneamente nei thread processor e non nei generatori

                System.out.println();
            }
            System.out.println("Thread " + threadID + " generated " + genCount + " values.");
        }
    }
}

class inputQueue {
    private ArrayList<Integer> queue;
    private int size;
    public int elements = 0; // è stato aggiunto per tenere conto degli elementi nella coda

    public inputQueue(int size) {
        queue = new ArrayList<>(size);
        this.size = size;
    }

    public synchronized int add(int val) { // è stato modificato il tipo di ritorno rispetto al codice consegnato
        try { // nel codice consegnato non c'era il try-catch
            while (queue.size() == size) { // è stato modificato il controllo rispetto al codice consegnato, in questo modo
                // i generatori possono inserire dati finché la coda non è piena, nel compito invece
                // i generatori dovevano aspettare che la coda si svuotasse
                wait();
            }
            queue.add(val);
            elements++;
            notifyAll();
            return 0;
        } catch (InterruptedException e) {
            return -1; // è stato inserito per gestire l'interruzione del thread
            //queando la coda è piena ed è in attesa
        }
    }

    public synchronized int remove(int id) {
        try { // nel codice consegnato non c'era il try-catch
            while (queue.isEmpty()) {
                wait();
            }

            int val = queue.removeFirst();
            elements--;
            notifyAll();
            return val;
        } catch (InterruptedException e) {
            return -1;
        }
    }

}

class threadProcessor extends Thread {
    private inputQueue[] queues;
    private int threadID;
    private int T, D, completed, remaining;
    private int extraction = 0;
    private int N;
    private outputQueue output;

    public threadProcessor(inputQueue[] queues, int threadID, int T, int N, outputQueue output) {
        this.queues = queues;
        this.threadID = threadID; // nel codice consegnato manca l'assegnamento di questo attributo
        this.T = T;
        this.N = N;
        this.output = output;
        completed = 0;
    }

    @Override
    public void run() {
        try {
            while (true){
                completed++; // è stato spostato qui rispetto al codice consegnato, poiché si perdeva il conto del
                // numero di computazioni completate quando il thread veniva interrotto a metà computazione
                int[] values = new int[N];
                int sum = 0;
                remaining = 0;
                for (int i = 0; i<N; i++) {
                    values[i] = queues[i].remove(threadID);
                    if (values[i] == -1) {
                        throw new InterruptedException();
                    }
                    sum += values[i];
                }
                extraction++;
                results res = new results(values, sum, extraction);
                D = (int) (Math.random() * 100); // manca il cast a int nel codice consegnato
                sleep(T+D);
                if ((output.add(res, threadID)) == -1){
                    throw new InterruptedException();
                }
            }
        }
        catch (InterruptedException e) {
            if (threadID == 0) {// è stato inserito per pulite l'output dato che le code sono comuni il risultato è lo stesso
                for (int i = 0; i < N; i++) {
                    remaining += queues[i].elements; // nella cosnsegna è presente un errore secondo cui si somma il valore di size
                    // è stato introdotto elements per tenere conto della dimensione delle code dinamicamente
                }
                remaining += output.elements;//è stato aggiunto per tenere conto degli elementi nella coda di output, non presente nella consegna
                System.out.println("remaining values in the queues: " + remaining);
                System.out.println();
            }
            System.out.println("Thread " + threadID + " completed " + completed + " computations.");
        }
    }
}

class outputQueue {
    private LinkedList<results> queue;
    private boolean[] acks;
    private int M;
    private int count;
    public int elements = 0;

    public outputQueue(int M) {
        this.M = M;
        queue = new LinkedList<results>();
        acks = new boolean[M];
    }

    public synchronized int add(results res, int id) { // è stato modificato il tipo di ritorno rispetto al codice consegnato
        try{ // è stato aggiunto il try-catch
            while (acks[id]){
                wait();
            }
            queue.addLast(res);
            acks[id] = true;
            elements++;
            notifyAll();
            return 0;
        }catch (InterruptedException e) {
            return -1; // la funzione è stata modificata poiché il thread rimaneva bloccato qui
            //inoltre si può seguire il flusso del codice consegnato, analogamente con la funzione add di inputQueue
        }
    }

    public synchronized results get(){
        try { // è stato aggiunto il try-catch
            while (queue.isEmpty()) {
                wait();
            }
            results res = queue.removeFirst();
            count++;
            elements--;
            if (count == M) {
                count = 0;
                acks = new boolean[M];
            }
            notifyAll();
            return res;
        } catch (InterruptedException e) {
            return null;
        }
    }
}

class threadPrinter extends Thread {
    private outputQueue output;
    private int printed = 0;

    public threadPrinter(outputQueue output) {
        this.output = output;
    }

    @Override
    public void run() {
        try {
            while (true) {
                results res = output.get();
                if (res == null) {
                    throw new InterruptedException();
                }
                System.out.println("Thread Printer n°extraction: " + res.extraction + " extracted the following values:" + Arrays.toString(res.results)+ " and sum: " + res.sum);
                printed++;
            }
        } catch (InterruptedException e) {
            System.out.println("Thread Printer printed " + printed + " results.");
        }
    }
}

class results {
    public int[] results;
    public int sum;
    public int extraction;

    public results(int[] values, int sum, int extraction) {
        this.results = values;
        this.sum = sum;
        this.extraction = extraction;
    }
}

public class Main {
    public static void main(String[] args) {
        int L = 10;
        int N = 4;
        int M =3; // nel codice consegnato è riferito come W
        int X = 200;
        int T = 100+(X*M); // questa relazione è  stata introdotta per far terminare il programma con dei valori nelle code
        // essendo i processor più veloci nell'estrazione rispetto ai generatori

        inputQueue[] queues = new inputQueue[N];
        threadGenerator[] generators = new threadGenerator[N];
        threadProcessor[] processors = new threadProcessor[M];
        outputQueue output = new outputQueue(M);
        threadPrinter printer = new threadPrinter(output);


        for (int i = 0; i < N; i++) {
            queues[i] = new inputQueue(L);
            generators[i] = new threadGenerator(queues[i], i, X, N);
        }

        for (int i = 0; i < M; i++) {
            processors[i] = new threadProcessor(queues, i, T, N, output);
        }

        for (int i = 0; i < N; i++) {
            generators[i].start();
        }

        for (int i = 0; i < M; i++) {
            processors[i].start();
        }

        printer.start();

        try {
            sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // sono stati raggruppati i for degli interrupt e join rispetto al codice consegnato

        System.out.println();

        System.out.println("---------- Shutting down thread Generator ----------");
        System.out.println();

        for (int i = 0; i < N; i++) {
            try {
                generators[i].interrupt();
                generators[i].join();
                System.out.println("****************************");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println();
        System.out.println("---------- Shutting down thread Processor ----------");
        System.out.println();

        for (int i = 0; i < M; i++) {
            try {
                processors[i].interrupt();
                processors[i].join();
                System.out.println("****************************");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println();
        System.out.println("---------- Shutting down thread Printer ----------");
        System.out.println();
        try{
            printer.interrupt();
            printer.join();
        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println();
        System.out.println("---------- End of the program ----------");
    }
}
