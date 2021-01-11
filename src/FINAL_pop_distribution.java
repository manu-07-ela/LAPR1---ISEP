import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Scanner;

public class FINAL_pop_distribution{
    static Scanner read = new Scanner(System.in);
    public static void main(String[] args) throws IOException, InterruptedException {
        //simulação da entrada por paramêtro do nome do ficheiro
        String fileName = read.next();
        String[] speciesName = speciesName(fileName);
        int size = sizeMatrix(fileName);
        double [] sizeSpecies = new double [size];
        double[][] leslieMatrix = new double[size][size];
        readFile(fileName, sizeSpecies, leslieMatrix);
        //print(sizeSpecies);
        //print1(leslieMatrix);
        int t = popDistribution(sizeSpecies, leslieMatrix);
        System.out.println(t);
        callGnuplot(t-1, size);
    }
    public static String[] speciesName (String path){
        String[] specie = path.split(".txt");
        return specie;
    }
    public static void readFile (String path, double[] size, double[][] leslie) throws FileNotFoundException {
        String vector;
        String [] auxVector;
        int cont=0, i;
        File archive = new File(path);
        Scanner readFile = new Scanner(archive);

        do{
            vector = readFile.nextLine();
            auxVector = transformVector(vector);
            switch (cont){
                case 0:
                    for (i=0; i<auxVector.length; i++){
                        size[i] = Integer.parseInt(auxVector[i]);
                    }
                    break;
                case 1:
                    for (i=0; i<leslie.length-1; i++) {
                        leslie[i+1][i] = Double.parseDouble(auxVector[i]);
                    }
                    break;
                default:
                    for (i=0; i<auxVector.length; i++){
                        leslie[0][i] = Double.parseDouble(auxVector[i]);
                    }
                    break;
            }
            cont++;
        }while (readFile.hasNextLine());

        readFile.close();
    }



    public static String[] transformVector (String vector){
        String[] auxVector = vector.split(", ");
        for (int i=0; i<auxVector.length; i++){
            auxVector[i] = auxVector[i].substring(auxVector[i].indexOf("=")+1);
        }
        return auxVector;
    }

    public static int sizeMatrix (String path) throws FileNotFoundException {
        File archive = new File(path);
        Scanner readFile = new Scanner(archive);
        String vector = readFile.nextLine();
        int size = transformVector(vector).length;
        readFile.close();
        return size;
    }

    public static void print (int[] size){
        for (int i=0; i< size.length; i++){
            System.out.printf("%d ", size[i]);
            System.out.println();
        }
    }

    public static void print1 (double[][] size){
        for (int i=0; i< size.length; i++){
            for (int k=0; k< size.length; k++){
                System.out.printf("%f ", size[i][k]);
            }
            System.out.println();
        }
    }

    public static int popDistribution (double initialPopVec[], double[][] leslieMatrix) throws IOException {
        Scanner read = new Scanner(System.in);

        //double initialPopVec[] = {1000, 300, 330, 100};
        //double[][] leslieMatrix = {{0.50,2.40,1,0},{0.5,0,0,0},{0,0.8,0,0},{0,0,0.5,0}};
        //double[][] leslieMatrix = {{0, 3, 3.17, 0.39}, {0.11, 0, 0, 0}, {0, 0.29, 0, 0}, {0, 0, 0.33, 0}};

        print1D(initialPopVec);
        printMatrix(leslieMatrix);

        int generationNum, t = 0;

        System.out.println("Insert the generations' number to be estimated: ");

        do {
            generationNum = read.nextInt();
        } while (generationNum <= 0);

        double[] popVec = new double[leslieMatrix.length];
        double[] normalizedPopVec = new double[popVec.length];

        double[][] distributionMatrix = new double[generationNum][leslieMatrix.length];
        double[][] normDistMatrix = new double[generationNum][leslieMatrix.length];

        double[] popDim = new double[generationNum];
        double[] rateVariation = new double[generationNum];

        double dim, rate;

        for(int time = 0; time < generationNum; time++) {

            //POPULATION DISTRIBUTION
            fillPopulationDistribution(initialPopVec,popVec,distributionMatrix,leslieMatrix,time);

            //NORMALIZATION
            fillNormalizedPopVec(normalizedPopVec,popVec,normDistMatrix,time);

            //DIMENSION
            dim=getTotalPopulation(popVec);
            fillArray(dim, time, popDim);
        }

        //RATE

        while(t+1<generationNum) {
            rate = getRateOfChangeOverTheYears(t, popDim);
            fillArray(rate, t, rateVariation);
            t++;
        }

        dimensionDataFormat(popDim, rateVariation);

        for (int i = 0; i < generationNum; i++) {
            printGenerationInfo(i,generationNum,distributionMatrix,normDistMatrix,popDim,rateVariation);
        }

        return t;
    }
    public static void generationsDataFormat (double [] popVec, double [] normalizedPopVec, int gen) throws IOException {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        int filesNum = popVec.length;
        String data = "", fn= "";
        for(int i=0;i<filesNum;i++){
            data =  gen + " " + df.format(popVec[i]) + " " + df.format(normalizedPopVec[i]);
            fn = "class"+(i+1)+".dat";
            dataToFile(fn, data);
        }
    }
    public static void dimensionDataFormat (double [] popDim, double [] rateVariation) throws IOException {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        String data = "";
        for(int k=0; k < popDim.length; k++){
            data = k + " " + df.format(popDim[k]) + " " + df.format(rateVariation[k]);
            dataToFile("populationTotal.dat", data);
        }
    }
    public static void dataToFile(String fileName, String fileData) throws IOException {
        String textToAppend = fileData;
        File file = new File(fileName);
        if(file.exists()){
            //Set true for append mode
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            writer.newLine();
            writer.write(textToAppend);
            writer.close();
        }else{
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(textToAppend);
            writer.close();
        }
    }
    public static void fillPopulationDistribution(double initialPopVec[], double[] popVec, double[][] distributionMatrix, double[][] leslieMatrix, int time) {
        double mult = 0;
        double[] previousPopVec = new double[popVec.length];

        if (time == 0) {
            for (int i = 0; i < initialPopVec.length; i++) {
                popVec[i] = initialPopVec[i];
            }
            fillMatrix(distributionMatrix,time,popVec);
        } else {
            fillPreviousPopVec(previousPopVec,popVec);
            for (int line = 0; line < leslieMatrix.length; line++) {
                for (int column = 0; column < leslieMatrix[line].length; column++) {
                    mult = mult + leslieMatrix[line][column] * previousPopVec[column];
                }
                popVec[line] = mult;
                mult = 0;
            }
            fillMatrix(distributionMatrix,time,popVec);
        }
    }
    public static void fillPreviousPopVec(double[] previousPopVec, double[] popVec) {
        for (int i = 0; i < popVec.length; i++) {
            previousPopVec[i] = popVec[i];
        }
    }
    public static double getTotalPopulation(double[] popVec) {
        double sum = 0;

        for (int i = 0; i < popVec.length; i++) {
            sum += popVec[i];
        }
        return sum;
    }
    public static void fillNormalizedPopVec(double[] normalizedPopVec, double[] popVec,double[][] normDistMatrix, int time) throws IOException {
        double totalPopulation = getTotalPopulation(popVec);

        for (int i = 0; i < popVec.length; i++) {
            normalizedPopVec[i] = popVec[i] / totalPopulation;
        }
        generationsDataFormat(popVec, normalizedPopVec, time);
        fillMatrix(normDistMatrix,time,normalizedPopVec);
    }
    /* -------------------------------- APAGAR ANTES DE ENTREGAR ------------------------------------------------------*/
    public static void printMatrix(double[][] array) {
        for (int line = 0; line < array.length; line++) {
            for (int column = 0; column < array[line].length; column++) {
                System.out.print(array[line][column] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
    public static void printPopDistribution(double[][] matrix, int time) {
        for (int line = 0; line < matrix.length; line++) {
            for (int column = 0; column < matrix[line].length; column++) {
                if(line == time) {
                    System.out.print("- Class " + column + ": ");
                    System.out.printf("%.3f%n", matrix[line][column]);
                }
            }
        }
        System.out.println();
    }
    public static void callGnuplot (int gen, int classes) throws IOException, InterruptedException {
        Process process1 = Runtime.getRuntime().exec("gnuplot -c ./testeGnuplot.gp 1 " + classes + " " + gen);
        process1.waitFor();
        deleteDatFiles();
    }
    public static void deleteDatFiles(){
        // Lists all files in folder
        File folder = new File("./");
        File fList[] = folder.listFiles();
        // Searchs .lck
        for (int i = 0; i < fList.length; i++) {
            String pes = String.valueOf(fList[i]);
            if (pes.endsWith(".dat")) {
                // and deletes
                boolean success = (new File(String.valueOf(fList[i])).delete());
            }
        }
    }
    /*-----------------------object = FILL POP DIMENSION AND RATE VARIATION OVER THE YEARS----------------------------*/
    public static void fillArray(double object, int time, double[] array) {
        array[time] = object;
    }
    public static void fillMatrix(double[][] distributionMatrix, int time, double[] popVec) {
        for (int line = time; line < (time+1); line++) {
            for (int column = 0; column < popVec.length; column++) {
                distributionMatrix[line][column] = popVec[column];
            }
        }
    }
    public static double getRateOfChangeOverTheYears(int time, double [] popDim) {
        double nowGeneration = popDim[time];
        double nextGeneration = popDim[time+1];

        double quocient = nextGeneration/nowGeneration;

        return quocient;
    }
    public static void printGenerationInfo(int time, int generationNum, double[][] distributionMatrix, double[][] normDistMatrix, double[] popDim, double[] rateVariation) {
        System.out.println("GENERATION " + time);
        System.out.println("Population Distribution:");
        printPopDistribution(distributionMatrix, time);
        System.out.println("Normalized Population Distribution:");
        printPopDistribution(normDistMatrix, time);
        System.out.println("Population Dimension: " + popDim[time]);
        if(time != generationNum-1) {
            System.out.println("Rate Variation between generation " + time + " and generation " + (time + 1) + ": " + rateVariation[time]);
        } else {
            System.out.println("For this generation, there is no Rate Variation.");
        }
        System.out.println();
    }

    //APAGAR ANTES DE ENTREGAR
    public static void print2D(double[][] array) {
        for (int line = 0; line < array.length; line++) {
            for (int column = 0; column < array[line].length; column++) {
                System.out.print(array[line][column] + " ");
            }
            System.out.println();
        }
    }
    //APAGAR ANTES DE ENTREGAR
    public static void print1D(double[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.println(array[i]);
        }
    }

}


