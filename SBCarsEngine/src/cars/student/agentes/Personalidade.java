package cars.student.agentes;

import java.awt.Color;
import java.util.List;

import static cars.student.agentes.Intencao.*;

/**
 * Os três tipos de sobrevivente. Todos usam os MESMOS estados;
 * o que muda são os pesos (o "volume" de cada vontade) e as distâncias.
 *
 * - pesoLutar / pesoFugir / pesoLootear: quanto cada vontade "fala alto".
 * - distanciaMinima: limite antes de começar a se afastar do zumbi.
 *   Também é onde o perigo chega a 100%.
 * - distanciaSegura: até onde ele foge antes de voltar a procurar itens.
 * - ordemDeEnfase: quem ganha quando duas vontades empatam (a primeira da lista).
 */
public enum Personalidade {

    LOUCO("O Louco", "Sede por combate, o foco dele é matar o zumbi, mesmo que chegue bem perto ele continua no combate",
            new Color(205, 45, 45),
            /* lutar, fugir, lootear */ 1.0, 0.3, 0.4,
            /* dist. mínima, dist. segura, segundos investigando */ 45, 230, 2.0,
            /* ênfase no empate */ LUTAR, LOOTEAR, FUGIR),

    MEDROSO("O Medroso", "Atira de longe, mas se chegar levemente perto, já começa a fugir.",
            new Color(40, 110, 220),
            0.6, 1.0, 0.6,
            160, 320, 2.0,
            FUGIR, LOOTEAR, LUTAR),

    CLEPTOMANIACO("O Cleptomaníaco", "Independente do perigo, na primeira oportunidade que tiver vai começar a procurar por loot.",
            new Color(150, 60, 190),
            0.5, 0.5, 1.0,
            110, 260, 1.2,
            LOOTEAR, FUGIR, LUTAR);

    private final String nome;
    private final String frase;
    private final Color cor;
    private final double pesoLutar;
    private final double pesoFugir;
    private final double pesoLootear;
    private final double distanciaMinima;
    private final double distanciaSegura;
    private final double tempoDeInvestigacao;
    private final List<Intencao> ordemDeEnfase;

    Personalidade(String nome, String frase, Color cor,
                  double pesoLutar, double pesoFugir, double pesoLootear,
                  double distanciaMinima, double distanciaSegura, double tempoDeInvestigacao,
                  Intencao... ordemDeEnfase) {
        this.nome = nome;
        this.frase = frase;
        this.cor = cor;
        this.pesoLutar = pesoLutar;
        this.pesoFugir = pesoFugir;
        this.pesoLootear = pesoLootear;
        this.distanciaMinima = distanciaMinima;
        this.distanciaSegura = distanciaSegura;
        this.tempoDeInvestigacao = tempoDeInvestigacao;
        this.ordemDeEnfase = List.of(ordemDeEnfase);
    }

    public double getPeso(Intencao intencao) {
        return switch (intencao) {
            case LUTAR -> pesoLutar;
            case FUGIR -> pesoFugir;
            case LOOTEAR -> pesoLootear;
        };
    }

    public String getNome() { return nome; }
    public String getFrase() { return frase; }
    public Color getCor() { return cor; }
    public double getDistanciaMinima() { return distanciaMinima; }
    public double getDistanciaSegura() { return distanciaSegura; }
    public double getTempoDeInvestigacao() { return tempoDeInvestigacao; }
    public List<Intencao> getOrdemDeEnfase() { return ordemDeEnfase; }
}
