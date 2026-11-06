# 🧟 A Caçada

Baseado em jogos survival horror, onde você precisa procurar por itens enquanto luta contra inimigos e foge para não morrer em um escritório fechado. Mas...que tal experienciar o outro lado?

Você controla (ou não) os zumbis, enquanto um sobrevivente controlado por IA explora um escritório procurando itens, atira em zumbis e foge quando começa a ficar encurralada. Do outro lado, zumbis patrulham o mapa, perseguem o alvo ao vê-lo começam a persegui-lo e tentar pegá-lo. 

Você pode escolher controlar o zumbi ou deixar ele no automático, além de também poder spawnar mais zumbis no mapa.

---

## 📋 Comportamentos, Pesos e Mapa

*   Os sobreviventes tem comportamento de Seek, Flee, Arrive, Wander e Obstacle Avoidance, enquanto os zumbis tem os mesmos, menos o Flee, além de ter também o Path Following, com mais detalhes no tópico de Steering Behaviours.
*   Como um adicional, os zumbis também tem o comportamento de Separation para evitar os zumbis se empilharem e ficarem um em cima do outro.
*   Além disso também foi criado um mapa com colisões e loot spawnado de forma procedural, não permitindo spawnar em cima de obstáculos ou fora do mapa.
*   Por fim, 6 combinações implementadas com pesos ajustados para movimentação natural, com mais detalhes no tópico de Combinações e Pesos.

---

## 🚀 Como Rodar

*   **No IntelliJ:** Abra a pasta `SBCarsEngine` e rode a classe `cars.engine.Window` (método `main`). Requer Java 21+.

Ao abrir o projeto, você tem a opção de escolher a personalidade do sobrevivente, podendo escolher entre O Louco, O Medroso e O Cleptomaníaco. Além disso, o painel de **Debug** abrirá ao lado permitindo visualizar distâncias, forças vetoriais, as linhas de colisão, a opção do modo automático e pesos em tempo real.

---

## 🧠 Os Steering Behaviors Explicados

A classe base `Agente` calcula todas as forças com pesos, limita a resultante e aplica na velocidade.

### Simples
*   **Seek:** Usado pelo zumbi em perseguição direta e pelo sobrevivente ao traçar uma rota para um ponto de exploração ou ponto de fuga.
*   **Flee:** Usado pelo sobrevivente. Ele gera uma força de repulsão (Flee) para cada zumbi visível.
*   **Arrive:** Usado para paradas suaves. O sobrevivente usa para chegar exatamente em cima de um item ou para frear e manter distância enquanto atira.
*   **Wander:** Adiciona um movimento orgânico. Usado pelo zumbi quando perde o alvo de vista ("farejando") e pelo sobrevivente enquanto explora (combinado com Seek).

### Avançados
*   **Obstacle Avoidance:** Três linhas (ou antenas) são projetadas à frente de todo agente. Se a antena detecta a parede do mapa, gera uma força repulsiva multiplicada pela normal da parede. O tamanho da antena escala com a velocidade.
*   **Path Following:** O zumbi projeta sua posição futura para prever se vai sair da rota de patrulha. Se sair, aplica um Seek para voltar à rota, marcada em azul, se estiver dentro, apenas mantém o trajeto pela rota.

---

## ⚖️ Combinações e Pesos

O projeto aplica dinâmicas específicas dependendo do estado do agente para garantir fluidez. O **Obstacle Avoidance** sempre entra com peso máximo (2.0) para garantir que desviar de paredes seja a prioridade absoluta.

| **Sobrevivente Fugindo** | Seek na rota (1.0) + Flee dos zumbis (0.5 cada) + OA (2.0) | Só usar Flee faria ele correr em linha reta e quicar nas paredes. O Seek traça a rota de fuga ideal, enquanto o Flee mantém a urgência de se afastar da ameaça. |
| **Sobrevivente Procurando** | Seek no destino (1.0) + Wander (0.5) + OA (2.0) | Só Wander o deixaria perdido, enquanto só Seek seria robótico. O Wander com peso menor adiciona naturalidade e curvas à caminhada até o objetivo. |
| **Zumbi Patrulhando** | Path Following (1.0) + Separation (1.5) + OA (2.0) | Segue a rota do mapa organicamente. O Separation alto impede que os zumbis andem encavalados, usando o raio da rota para andarem lado a lado. |
| **Zumbi Perseguindo** | Seek no sobrevivente (1.0) + Separation (1.5) + OA (2.0) | Corrida direta ao alvo, mas mantendo o distanciamento entre a própria horda para tentar cercar o jogador. |

---

## 🎭 Padrão State

A tomada de decisão usa o padrão State, similar ao já utilizado no projeto Juca e Bob das aulas e no último projeto. Não há switches gigantes, cada estado decide suas próprias transições, facilitando a escalabilidade do projeto, além disso tudo está muito bem separado em pastas, facilitando encontrar as funções e classes pelo projeto.

### Estados do Sobrevivente
O sobrevivente toma decisões baseadas na sua Personalidade. "O Louco" tem peso alto para *Lutar*, enquanto "O Medroso" tem peso alto para *Fugir* e "O Cleptomaniaco" tem peso alto para *Lootear* alterando como as transições ocorrem dependendo do personagem .

```mermaid
stateDiagram-Sobrevivente
    [*] --> Procurando
    Procurando --> IndoAoItem: Vê item (Lootear vence)
    IndoAoItem --> Investigando: Chegou no item
    Investigando --> Procurando: Pegou o item
    Procurando --> Atirando: Vê zumbi (Lutar vence)
    Atirando --> Fugindo: Zumbi aproximou muito (Fugir vence)
    Fugindo --> Procurando: Distância segura atingida
    Fugindo --> Encurralado: Sem rota de fuga
    Encurralado --> Fugindo: Achou saída
```

### 🧟 Estados do Zumbi
```mermaid
stateDiagram-Zumbi
    [*] --> Patrulhando
    Patrulhando --> Perseguindo: Viu o sobrevivente
    Patrulhando --> Vagando: Rota bloqueada
    Perseguindo --> Vagando: Perdeu de vista por 4s
    Vagando --> Perseguindo: Viu o sobrevivente
    Vagando --> Patrulhando: Encontrou a rota após 5s
    Patrulhando --> Controlado: Jogador assumiu o controle
```

---

## 🤖 Uso de IA

*   Para esse projeto, foi usado o Claude para auxilio durante a produção do código, especialmente na parte de visual, como UI, design do mapa, personagens e linhas de debugging.

---