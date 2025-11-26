import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ExerciciosParalelos {

    static class Exercicio4 {
        
        // Versão Sequencial
        static class ResultadoEstatistico {
            double media;
            double desvioPadrao;
            long tempo;
            
            ResultadoEstatistico(double media, double desvioPadrao, long tempo) {
                this.media = media;
                this.desvioPadrao = desvioPadrao;
                this.tempo = tempo;
            }
        }
        
        public static ResultadoEstatistico calcularSequencial(double[] vetor) {
            long inicio = System.nanoTime();
            
            // Primeira passagem: calcular soma e média
            double soma = 0;
            for (double valor : vetor) {
                soma += valor;
            }
            double media = soma / vetor.length;
            
            // Segunda passagem: calcular soma dos quadrados das diferenças
            double somaQuadrados = 0;
            for (double valor : vetor) {
                double diferenca = valor - media;
                somaQuadrados += diferenca * diferenca;
            }
            
            double desvioPadrao = Math.sqrt(somaQuadrados / vetor.length);
            long tempo = System.nanoTime() - inicio;
            
            return new ResultadoEstatistico(media, desvioPadrao, tempo);
        }
        
        // Versão Paralela
        public static ResultadoEstatistico calcularParalelo(double[] vetor, int numThreads) 
                throws InterruptedException {
            long inicio = System.nanoTime();
            
            int tamanhoBloco = vetor.length / numThreads;
            Thread[] threads = new Thread[numThreads];
            double[] somasParciais = new double[numThreads];
            
            // Primeira fase: calcular somas parciais para média
            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                final int inicioBloco = i * tamanhoBloco;
                final int fimBloco = (i == numThreads - 1) ? vetor.length : (i + 1) * tamanhoBloco;
                
                threads[i] = new Thread(() -> {
                    double somaParcial = 0;
                    for (int j = inicioBloco; j < fimBloco; j++) {
                        somaParcial += vetor[j];
                    }
                    somasParciais[threadId] = somaParcial;
                });
                threads[i].start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            double somaTotal = 0;
            for (double soma : somasParciais) {
                somaTotal += soma;
            }
            double media = somaTotal / vetor.length;
            
            // Segunda fase: calcular somas parciais dos quadrados das diferenças
            double[] somasQuadradosParciais = new double[numThreads];
            
            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                final int inicioBloco = i * tamanhoBloco;
                final int fimBloco = (i == numThreads - 1) ? vetor.length : (i + 1) * tamanhoBloco;
                final double mediaFinal = media;
                
                threads[i] = new Thread(() -> {
                    double somaQuadradosParcial = 0;
                    for (int j = inicioBloco; j < fimBloco; j++) {
                        double diferenca = vetor[j] - mediaFinal;
                        somaQuadradosParcial += diferenca * diferenca;
                    }
                    somasQuadradosParciais[threadId] = somaQuadradosParcial;
                });
                threads[i].start();
            }
            
            // Aguardar conclusão
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Calcular desvio padrão total
            double somaQuadradosTotal = 0;
            for (double somaQuadrados : somasQuadradosParciais) {
                somaQuadradosTotal += somaQuadrados;
            }
            double desvioPadrao = Math.sqrt(somaQuadradosTotal / vetor.length);
            
            long tempo = System.nanoTime() - inicio;
            return new ResultadoEstatistico(media, desvioPadrao, tempo);
        }
        
        public static void executar() throws InterruptedException {
            System.out.println("=== EXERCÍCIO 4: MÉDIA E DESVIO PADRÃO ===\n");
            
            int[] tamanhos = {1_000_000, 5_000_000, 10_000_000};
            int[] numThreadsList = {2, 4, 8};
            
            for (int tamanho : tamanhos) {
                System.out.println("Tamanho do vetor: " + tamanho);
                
                // Gerar vetor aleatório
                double[] vetor = new double[tamanho];
                Random random = new Random(42);
                for (int i = 0; i < tamanho; i++) {
                    vetor[i] = random.nextDouble() * 100;
                }
                
                // Versão sequencial
                ResultadoEstatistico resSeq = calcularSequencial(vetor);
                System.out.printf("Sequencial: Média=%.4f, Desvio=%.4f, Tempo=%.3f ms\n",
                        resSeq.media, resSeq.desvioPadrao, resSeq.tempo / 1_000_000.0);
                
                // Versões paralelas
                for (int numThreads : numThreadsList) {
                    ResultadoEstatistico resPar = calcularParalelo(vetor, numThreads);
                    double speedup = (double) resSeq.tempo / resPar.tempo;
                    double eficiencia = speedup / numThreads;
                    
                    System.out.printf("Paralelo (%d threads): Média=%.4f, Desvio=%.4f, Tempo=%.3f ms, " +
                            "Speedup=%.2fx, Eficiência=%.2f%%\n",
                            numThreads, resPar.media, resPar.desvioPadrao, 
                            resPar.tempo / 1_000_000.0, speedup, eficiencia * 100);
                }
                System.out.println();
            }
        }
    }
    
    static class Exercicio5 {
        
        static class ResultadoMultiplicacao {
            double[] resultado;
            long tempo;
            
            ResultadoMultiplicacao(double[] resultado, long tempo) {
                this.resultado = resultado;
                this.tempo = tempo;
            }
        }
        
        // Versão Sequencial
        public static ResultadoMultiplicacao multiplicarSequencial(double[][] matriz, double[] vetor) {
            long inicio = System.nanoTime();
            int n = matriz.length;
            double[] resultado = new double[n];
            
            for (int i = 0; i < n; i++) {
                double soma = 0;
                for (int j = 0; j < n; j++) {
                    soma += matriz[i][j] * vetor[j];
                }
                resultado[i] = soma;
            }
            
            long tempo = System.nanoTime() - inicio;
            return new ResultadoMultiplicacao(resultado, tempo);
        }
        
        // Versão Paralela
        public static ResultadoMultiplicacao multiplicarParalelo(double[][] matriz, double[] vetor, 
                int numThreads) throws InterruptedException {
            long inicio = System.nanoTime();
            int n = matriz.length;
            double[] resultado = new double[n];
            
            int linhasPorThread = n / numThreads;
            Thread[] threads = new Thread[numThreads];
            for (int i = 0; i < numThreads; i++) {
                final int inicioLinha = i * linhasPorThread;
                final int fimLinha = (i == numThreads - 1) ? n : (i + 1) * linhasPorThread;
                
                threads[i] = new Thread(() -> {
                    for (int linha = inicioLinha; linha < fimLinha; linha++) {
                        double soma = 0;
                        for (int col = 0; col < n; col++) {
                            soma += matriz[linha][col] * vetor[col];
                        }
                        resultado[linha] = soma;
                    }
                });
                threads[i].start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            long tempo = System.nanoTime() - inicio;
            return new ResultadoMultiplicacao(resultado, tempo);
        }
        
        public static void executar() throws InterruptedException {
            System.out.println("=== EXERCÍCIO 5: MULTIPLICAÇÃO MATRIZ × VETOR ===\n");
            
            int[] tamanhos = {1000, 2000, 3000};
            int[] numThreadsList = {2, 4, 8};
            
            for (int n : tamanhos) {
                System.out.println("Tamanho da matriz: " + n + "×" + n);
                
                // Gerar matriz e vetor aleatórios
                double[][] matriz = new double[n][n];
                double[] vetor = new double[n];
                Random random = new Random(42);
                
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        matriz[i][j] = random.nextDouble();
                    }
                    vetor[i] = random.nextDouble();
                }
                
                // Versão sequencial
                ResultadoMultiplicacao resSeq = multiplicarSequencial(matriz, vetor);
                System.out.printf("Sequencial: Tempo=%.3f ms\n", resSeq.tempo / 1_000_000.0);
                
                // Versões paralelas
                for (int numThreads : numThreadsList) {
                    ResultadoMultiplicacao resPar = multiplicarParalelo(matriz, vetor, numThreads);
                    double speedup = (double) resSeq.tempo / resPar.tempo;
                    double eficiencia = speedup / numThreads;
                    
                    System.out.printf("Paralelo (%d threads): Tempo=%.3f ms, Speedup=%.2fx, " +
                            "Eficiência=%.2f%%\n",
                            numThreads, resPar.tempo / 1_000_000.0, speedup, eficiencia * 100);
                }
                System.out.println();
            }
        }
    }
    
    static class Exercicio6 {
        
        static class ResultadoPrimos {
            int count;
            long tempo;
            
            ResultadoPrimos(int count, long tempo) {
                this.count = count;
                this.tempo = tempo;
            }
        }
        
        // Verificar se um número é primo
        private static boolean ehPrimo(int n) {
            if (n < 2) return false;
            if (n == 2) return true;
            if (n % 2 == 0) return false;
            
            int limite = (int) Math.sqrt(n);
            for (int i = 3; i <= limite; i += 2) {
                if (n % i == 0) return false;
            }
            return true;
        }
        
        // Versão Sequencial
        public static ResultadoPrimos contarPrimosSequencial(int n) {
            long inicio = System.nanoTime();
            int count = 0;
            
            for (int i = 1; i <= n; i++) {
                if (ehPrimo(i)) count++;
            }
            
            long tempo = System.nanoTime() - inicio;
            return new ResultadoPrimos(count, tempo);
        }
        
        // Versão Paralela - Partição Estática
        public static ResultadoPrimos contarPrimosParaleloEstatico(int n, int numThreads) 
                throws InterruptedException {
            long inicio = System.nanoTime();
            int[] countsParciais = new int[numThreads];
            Thread[] threads = new Thread[numThreads];
            
            int numeroPorThread = n / numThreads;
            
            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                final int inicioRange = i * numeroPorThread + 1;
                final int fimRange = (i == numThreads - 1) ? n : (i + 1) * numeroPorThread;
                
                threads[i] = new Thread(() -> {
                    int countParcial = 0;
                    for (int num = inicioRange; num <= fimRange; num++) {
                        if (ehPrimo(num)) countParcial++;
                    }
                    countsParciais[threadId] = countParcial;
                });
                threads[i].start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            int countTotal = 0;
            for (int count : countsParciais) {
                countTotal += count;
            }
            
            long tempo = System.nanoTime() - inicio;
            return new ResultadoPrimos(countTotal, tempo);
        }
        
        // Versão Paralela - Partição Dinâmica
        public static ResultadoPrimos contarPrimosParaleloDinamico(int n, int numThreads) 
                throws InterruptedException {
            long inicio = System.nanoTime();
            AtomicInteger proximoNumero = new AtomicInteger(1);
            AtomicInteger countTotal = new AtomicInteger(0);
            Thread[] threads = new Thread[numThreads];
            
            int tamanhoBloco = 1000;
            
            for (int i = 0; i < numThreads; i++) {
                threads[i] = new Thread(() -> {
                    while (true) {
                        int inicio_bloco = proximoNumero.getAndAdd(tamanhoBloco);
                        if (inicio_bloco > n) break;
                        
                        int fim_bloco = Math.min(inicio_bloco + tamanhoBloco - 1, n);
                        int countLocal = 0;
                        
                        for (int num = inicio_bloco; num <= fim_bloco; num++) {
                            if (ehPrimo(num)) countLocal++;
                        }
                        
                        countTotal.addAndGet(countLocal);
                    }
                });
                threads[i].start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            long tempo = System.nanoTime() - inicio;
            return new ResultadoPrimos(countTotal.get(), tempo);
        }
        
        public static void executar() throws InterruptedException {
            System.out.println("=== EXERCÍCIO 6: CONTAGEM DE NÚMEROS PRIMOS ===\n");
            
            int[] limites = {100_000, 500_000, 1_000_000};
            int[] numThreadsList = {2, 4, 8};
            
            for (int limite : limites) {
                System.out.println("Limite: " + limite);
                
                // Versão sequencial
                ResultadoPrimos resSeq = contarPrimosSequencial(limite);
                System.out.printf("Sequencial: Count=%d, Tempo=%.3f ms\n",
                        resSeq.count, resSeq.tempo / 1_000_000.0);
                
                // Versões paralelas
                for (int numThreads : numThreadsList) {
                    ResultadoPrimos resEstatico = contarPrimosParaleloEstatico(limite, numThreads);
                    double speedupEstatico = (double) resSeq.tempo / resEstatico.tempo;
                    double eficienciaEstatico = speedupEstatico / numThreads;
                    
                    System.out.printf("Paralelo Estático (%d threads): Count=%d, Tempo=%.3f ms, " +
                            "Speedup=%.2fx, Eficiência=%.2f%%\n",
                            numThreads, resEstatico.count, resEstatico.tempo / 1_000_000.0, 
                            speedupEstatico, eficienciaEstatico * 100);
                    
                    ResultadoPrimos resDinamico = contarPrimosParaleloDinamico(limite, numThreads);
                    double speedupDinamico = (double) resSeq.tempo / resDinamico.tempo;
                    double eficienciaDinamico = speedupDinamico / numThreads;
                    
                    System.out.printf("Paralelo Dinâmico (%d threads): Count=%d, Tempo=%.3f ms, " +
                            "Speedup=%.2fx, Eficiência=%.2f%%\n",
                            numThreads, resDinamico.count, resDinamico.tempo / 1_000_000.0, 
                            speedupDinamico, eficienciaDinamico * 100);
                }
                System.out.println();
            }
        }
    }
    
    static class Exercicio7 {
        
        static class ResultadoFiltro {
            int[][] imagemSaida;
            long tempo;
            
            ResultadoFiltro(int[][] imagemSaida, long tempo) {
                this.imagemSaida = imagemSaida;
                this.tempo = tempo;
            }
        }
        
        // Aplicar blur 3x3 em uma posição
        private static int aplicarBlur(int[][] imagem, int i, int j) {
            int soma = 0;
            int count = 0;
            
            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    int ni = i + di;
                    int nj = j + dj;
                    
                    if (ni >= 0 && ni < imagem.length && nj >= 0 && nj < imagem[0].length) {
                        soma += imagem[ni][nj];
                        count++;
                    }
                }
            }
            
            return soma / count;
        }
        
        // Versão Sequencial
        public static ResultadoFiltro aplicarFiltroSequencial(int[][] imagem) {
            long inicio = System.nanoTime();
            int altura = imagem.length;
            int largura = imagem[0].length;
            int[][] imagemSaida = new int[altura][largura];
            
            for (int i = 0; i < altura; i++) {
                for (int j = 0; j < largura; j++) {
                    imagemSaida[i][j] = aplicarBlur(imagem, i, j);
                }
            }
            
            long tempo = System.nanoTime() - inicio;
            return new ResultadoFiltro(imagemSaida, tempo);
        }
        
        // Versão Paralela
        public static ResultadoFiltro aplicarFiltroParalelo(int[][] imagem, int numThreads) 
                throws InterruptedException {
            long inicio = System.nanoTime();
            int altura = imagem.length;
            int largura = imagem[0].length;
            int[][] imagemSaida = new int[altura][largura];
            
            int linhasPorThread = altura / numThreads;
            Thread[] threads = new Thread[numThreads];
            
            for (int i = 0; i < numThreads; i++) {
                final int inicioLinha = i * linhasPorThread;
                final int fimLinha = (i == numThreads - 1) ? altura : (i + 1) * linhasPorThread;
                
                threads[i] = new Thread(() -> {
                    for (int linha = inicioLinha; linha < fimLinha; linha++) {
                        for (int col = 0; col < largura; col++) {
                            imagemSaida[linha][col] = aplicarBlur(imagem, linha, col);
                        }
                    }
                });
                threads[i].start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            long tempo = System.nanoTime() - inicio;
            return new ResultadoFiltro(imagemSaida, tempo);
        }
        
        public static void executar() throws InterruptedException {
            System.out.println("=== EXERCÍCIO 7: FILTRO EM IMAGEM (BLUR) ===\n");
            
            int[] tamanhos = {1000, 2000, 3000};
            int[] numThreadsList = {2, 4, 8};
            
            for (int tamanho : tamanhos) {
                System.out.println("Tamanho da imagem: " + tamanho + "×" + tamanho);
                
                // Gerar imagem aleatória
                int[][] imagem = new int[tamanho][tamanho];
                Random random = new Random(42);
                for (int i = 0; i < tamanho; i++) {
                    for (int j = 0; j < tamanho; j++) {
                        imagem[i][j] = random.nextInt(256);
                    }
                }
                
                // Versão sequencial
                ResultadoFiltro resSeq = aplicarFiltroSequencial(imagem);
                System.out.printf("Sequencial: Tempo=%.3f ms\n", resSeq.tempo / 1_000_000.0);
                
                // Versões paralelas
                for (int numThreads : numThreadsList) {
                    ResultadoFiltro resPar = aplicarFiltroParalelo(imagem, numThreads);
                    double speedup = (double) resSeq.tempo / resPar.tempo;
                    double eficiencia = speedup / numThreads;
                    
                    System.out.printf("Paralelo (%d threads): Tempo=%.3f ms, Speedup=%.2fx, " +
                            "Eficiência=%.2f%%\n",
                            numThreads, resPar.tempo / 1_000_000.0, speedup, eficiencia * 100);
                }
                System.out.println();
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            System.out.println("EXERCÍCIOS DE PROGRAMAÇÃO PARALELA EM JAVA\n");
            System.out.println("=" .repeat(60) + "\n");
            
            Exercicio4.executar();
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            Exercicio5.executar();
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            Exercicio6.executar();
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            Exercicio7.executar();
            
        } catch (InterruptedException e) {
            System.err.println("Erro na execução: " + e.getMessage());
            e.printStackTrace();
        }
    }
}