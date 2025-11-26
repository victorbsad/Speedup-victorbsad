# Exercícios de Programação Paralela

Este arquivo contém 4 exercícios demonstrando diferentes estratégias de paralelização em Java.

## Estrutura do Código

Cada exercício possui duas implementações:
- **Versão Sequencial**: Implementação tradicional (baseline para comparação)
- **Versão Paralela**: Implementação usando múltiplas threads

## Exercício 4: Cálculo de Média e Desvio Padrão

**Problema**: Calcular média e desvio padrão de um vetor de 100 milhões de elementos.

### Estratégia de Paralelização
- **Abordagem**: Two-phase parallel reduction
- **Fase 1**: Redução paralela para calcular soma (depois média)
  - Cada thread processa um bloco contíguo do vetor
  - Acumula soma parcial independentemente (sem contenção)
  - Barreira de sincronização (join) antes de calcular média global
- **Fase 2**: Redução paralela para calcular soma dos quadrados das diferenças
  - Usa média calculada na fase 1
  - Novamente cada thread processa seu bloco independentemente

### Divisão de Trabalho
- **Particionamento estático**: Vetor dividido em blocos de tamanho fixo
- **Fórmula**: `tamanhoBloco = N / numThreads`
- **Comunicação**: Arrays de resultados parciais (`somasParciais[]`)

### Sincronização
- **Thread.join()**: Barreira entre fase 1 e fase 2
- **Sem contenção**: Cada thread escreve em posição única do array de resultados

### Desempenho Esperado
- **Speedup teórico**: Próximo a N (número de threads)
- **Limitações**: 
  - Fase sequencial de redução (soma dos resultados parciais)
  - Largura de banda de memória (memory-bound)
- **Eficiência**: Alta (>80%) para grandes vetores

---

## Exercício 5: Multiplicação Matriz × Vetor

**Problema**: Multiplicar matriz 10.000×10.000 por vetor de 10.000 elementos.

### Estratégia de Paralelização
- **Abordagem**: Row-wise parallelization (paralelização por linhas)
- **Cada linha**: Cálculo independente (produto escalar linha × vetor)
- **Sem dependências**: Linhas diferentes podem ser processadas simultaneamente

### Divisão de Trabalho
- **Particionamento estático por linhas**
- **Fórmula**: `linhasPorThread = N / numThreads`
- **Acesso à memória**: 
  - Linhas da matriz: acesso contíguo (cache-friendly)
  - Vetor: compartilhado por todas as threads (somente leitura)

### Sincronização
- **Sem race condition**: Cada thread escreve em posições distintas do vetor resultado
- **Memória compartilhada**: Vetor de entrada (read-only) e resultado (write-disjoint)
- **Thread.join()**: Aguarda conclusão de todas as threads

### Desempenho Esperado
- **Speedup teórico**: Linear até saturar largura de banda
- **Limitações**:
  - Memory-bound (limitado por acesso à memória)
  - Cache thrashing se vetor não cabe em cache
- **Eficiência**: Moderada (~60-70%) devido a acesso intensivo à memória

---

## Exercício 6: Contagem de Números Primos

**Problema**: Contar números primos no intervalo [1, 10.000.000].

### Versão 1: Partição Estática

#### Estratégia
- **Abordagem**: Range-based static partitioning
- **Divisão**: Intervalo [1,N] dividido em subintervalos contíguos
- **Fórmula**: Cada thread processa `[inicio, inicio + numeroPorThread)`

#### Vantagens
- ✅ Implementação simples
- ✅ Sem overhead de sincronização durante processamento
- ✅ Localidade de cache (cada thread trabalha em range contíguo)

#### Desvantagens
- ❌ **Desbalanceamento de carga**: Intervalos maiores têm menos primos (densidade desigual)
- ❌ Thread que processa [1-1M] termina antes da que processa [9M-10M]
- ❌ Eficiência cai com distribuição não-uniforme de carga

#### Sincronização
- **Redução final**: Soma contadores locais (`countsParciais[]`)
- **Thread.join()**: Barreira de sincronização no final

### Versão 2: Partição Dinâmica

#### Estratégia
- **Abordagem**: Work-stealing com blocos de tamanho fixo
- **Threads**: Pegam blocos sob demanda via `AtomicInteger`
- **Granularidade**: Blocos de 1000 números (ajustável)

#### Vantagens
- ✅ **Balanceamento automático**: Thread lenta não atrasa outras
- ✅ Adapta-se a heterogeneidade (threads mais rápidas processam mais blocos)
- ✅ Eficiência maior em cargas desbalanceadas

#### Desvantagens
- ❌ **Overhead de sincronização**: `AtomicInteger.getAndAdd()` em cada bloco
- ❌ Menos localidade de cache (threads podem pular entre ranges)
- ❌ Contenção no `proximoNumero` se blocos forem muito pequenos

#### Sincronização
- **AtomicInteger proximoNumero**: Coordena alocação de blocos
- **AtomicInteger countTotal**: Acumula contagem thread-safe
- **Lock-free**: Operações atômicas evitam uso de locks explícitos

#### Trade-off de Granularidade
- **Blocos grandes**: Menos overhead, mais desbalanceamento
- **Blocos pequenos**: Melhor balanceamento, mais overhead atômico
- **Valor ideal**: Depende de N, numThreads, e custo de `ehPrimo()`

### Comparação de Desempenho
- **Estático**: Melhor se carga for uniforme (números aleatórios)
- **Dinâmico**: Melhor se carga for heterogênea (primos têm distribuição irregular)
- **Speedup esperado**: 
  - Estático: ~60-70% linear (devido a desbalanceamento)
  - Dinâmico: ~80-90% linear (melhor balanceamento compensa overhead)

---

## Exercício 7: Filtro de Desfoque em Imagem

**Problema**: Aplicar filtro de blur 3×3 em imagem 2000×2000 pixels.

### Estratégia de Paralelização
- **Abordagem**: Domain decomposition (decomposição por domínio)
- **Divisão**: Imagem dividida em faixas horizontais
- **Computação**: Stencil computation (cada pixel depende de vizinhos)

### Divisão de Trabalho
- **Particionamento estático por linhas**
- **Cada thread**: Processa faixa de linhas contíguas
- **Independência**: Cálculo de pixels não interfere entre threads

### Sincronização
- **Sem race condition**: Escrita disjunta (threads escrevem em linhas diferentes)
- **Memória compartilhada**:
  - Imagem entrada: somente leitura (todas as threads)
  - Imagem saída: escrita disjunta (cada thread sua faixa)
- **Thread.join()**: Aguarda conclusão antes de retornar resultado

### Características
- **Problema embarrassingly parallel**: Ideal para paralelização
- **Acesso à memória**: 
  - Leitura: kernel 3×3 ao redor de cada pixel
  - Escrita: um pixel por iteração
- **Localidade de cache**: Boa (acesso a linhas vizinhas)

### Desempenho Esperado
- **Speedup teórico**: Próximo a linear (problema CPU-bound)
- **Limitações**:
  - Falsas compartilhações (false sharing) se linhas estiverem em mesma cache line
  - Overhead de criação de threads
- **Eficiência**: Alta (>85%) - problema altamente paralelizável

### Otimizações Possíveis
- **Padding**: Adicionar espaço entre linhas para evitar false sharing
- **Thread pool**: Reusar threads em vez de criar/destruir
- **SIMD**: Vetorização do loop interno (processar múltiplos pixels simultaneamente)

---

## Métricas de Desempenho

### Speedup
```
Speedup = Tempo Sequencial / Tempo Paralelo
```
- **Ideal**: Speedup = N (número de threads)
- **Real**: Sempre menor devido a overhead e partes sequenciais

### Eficiência
```
Eficiência = Speedup / N × 100%
```
- **100%**: Paralelização perfeita (teórico)
- **>80%**: Boa paralelização
- **<50%**: Overhead ou desbalanceamento significativo

### Lei de Amdahl
```
Speedup Máximo = 1 / (S + P/N)
```
Onde:
- **S**: Fração sequencial do código
- **P**: Fração paralelizável
- **N**: Número de threads

**Conclusão**: Mesmo pequenas partes sequenciais limitam speedup máximo.

---

## Execução

### Compilar
```bash
javac ExerciciosParalelos.java -d bin
```

### Executar
```bash
java -cp bin ExerciciosParalelos
```

### Saída Esperada
Cada exercício exibe:
- Tempo de execução (sequencial e paralelo)
- Speedup obtido
- Eficiência percentual

---

## Conceitos de Programação Paralela

### Particionamento
- **Estático**: Divisão pré-determinada do trabalho
- **Dinâmico**: Threads pegam tarefas sob demanda

### Sincronização
- **Barreira**: Todas as threads esperam até última chegar (join)
- **Atômica**: Operações indivisíveis (AtomicInteger)
- **Lock-free**: Sincronização sem uso de locks explícitos

### Problemas Comuns
- **Race condition**: Múltiplas threads acessam/modificam mesma variável
- **Deadlock**: Threads esperam mutuamente por recursos
- **False sharing**: Threads acessam variáveis em mesma cache line
- **Load imbalance**: Threads com cargas de trabalho desiguais

### Padrões de Paralelização
- **Data parallelism**: Mesma operação em dados diferentes
- **Task parallelism**: Operações diferentes executadas simultaneamente
- **Pipeline**: Estágios de processamento em sequência
- **Reduction**: Combinar resultados parciais em resultado final
