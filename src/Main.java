import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static java.lang.Thread.sleep;

public class Main {
    public static void main(String[] args) {
        int L = 30;
        int N = 4;
        int M =3; // nel codice consegnato è riferito come W
        int T = 100;
        int X = 500;

        inputQueue[] queues = new inputQueue[N];
        threadGenerator[] generators = new threadGenerator[N];
        threadProcessor[] processors = new threadProcessor[M];
        outputQueue output = new outputQueue(M);
        threadPrinter printer = new threadPrinter(output);


        for (int i = 0; i < N; i++) {
            queues[i] = new inputQueue(L);
            generators[i] = new threadGenerator(queues[i], i, X);
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

        System.out.println("---------- Shutting down thread Generator ----------");

        for (int i = 0; i < N; i++) {
            try {
                generators[i].interrupt();
                generators[i].join();
                System.out.println("****************************");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("---------- Shutting down thread Processor ----------");

        for (int i = 0; i < M; i++) {
            try {
                processors[i].interrupt();
                processors[i].join();
                System.out.println("****************************");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("---------- Shutting down thread Printer ----------");
        try{
        printer.interrupt();
            printer.join();
        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("---------- End of the program ----------");
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

class threadGenerator extends Thread {
    private inputQueue uniqueQueue;
    private int threadID;
    private int val;
    private int X;
    private int genCount = 0;

    public threadGenerator(inputQueue uniqueQueue, int threadID, int sleepTime) {
        this.uniqueQueue = uniqueQueue;
        this.threadID = threadID;
        this.val = this.threadID;
        this.X = sleepTime;
    }

    @Override
    public void run() {
        try{
            while(true) {
                val++;
                if(uniqueQueue.add(val, threadID, genCount)==-1){
                    throw new InterruptedException();
                }
                genCount++;
                sleep(X);
            }
        }catch (InterruptedException e) {
            System.out.println("Thread " + threadID + " generated " + genCount + " values.");
            System.out.println("Thread " + threadID + " interrupted.");
        }
    }
}

class inputQueue {
    private ArrayList<Integer> queue;
    public int elements = 0; // è stato aggiunto per tenere conto degli elementi nella coda
    // è stato rimosso il parametro size dal costruttore poiché non viene utilizzato

    public inputQueue(int size) {
        queue = new ArrayList<>(size);
    }

    public synchronized int add(int val, int id, int genCount) {
        try { // nel codice consegnato non c'era il try-catch
            while (!queue.isEmpty()) {
                wait();
            }
            queue.add(val);
            elements++;
            notifyAll();
            return 0;
        } catch (InterruptedException e) {
            return -1;
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
                completed++;
            }
        }
        catch (InterruptedException e) {
            for (int i = 0; i < N; i++) {
                remaining += queues[i].elements; // nella cosnsegna è presente un errore secondo cui si somma il valore di size
                // è stato introdotto elements per tenere conto della dimensione delle code dinamicamente
            }
            remaining += output.elements; //è stato aggiunto per tenere conto degli elementi nella coda di output, non presente nella consegna
            System.out.println("Thread " + threadID + " completed " + completed + " computations.");
            System.out.println("remaining values: " + remaining);
        }
    }
}

class outputQueue {
    public LinkedList<results> queue;
    private boolean[] acks;
    private int M;
    private int count;
    public int elements = 0;

    public outputQueue(int M) {
        this.M = M;
        queue = new LinkedList<results>();
        acks = new boolean[M];
    }

    public synchronized int add(results res, int id) {
        try{
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
            //inoltre si può seguire il flusso del codice consegnato
        }
    }

    public synchronized results get(){
        try {
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
            System.out.println("Thread Printer interrupted.");
        }
    }
}