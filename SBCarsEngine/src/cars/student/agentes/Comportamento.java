package cars.student.agentes;

import java.awt.Color;

/**
 * Cada "cordinha" que pode puxar um agente. Serve para separar as forças
 * no debug: cada comportamento é desenhado como uma seta da sua cor.
 *
 * Simples: SEEK, FLEE, ARRIVE, WANDER.
 * Avançados: SEPARATION, OBSTACLE_AVOIDANCE, PATH_FOLLOWING.
 * CONTROLE não é steering behavior: é a força das setas do teclado (zumbi do jogador).
 */
public enum Comportamento {
    SEEK("Seek (buscar)", new Color(220, 40, 40)),
    FLEE("Flee (fugir)", new Color(190, 0, 190)),
    ARRIVE("Arrive (chegar suave)", new Color(20, 150, 60)),
    WANDER("Wander (vagar)", new Color(0, 150, 210)),
    SEPARATION("Separation (separar)", new Color(255, 130, 0)),
    OBSTACLE_AVOIDANCE("Obstacle Avoidance (desviar)", new Color(110, 110, 110)),
    PATH_FOLLOWING("Path Following (seguir rota)", new Color(0, 160, 160)),
    CONTROLE("Controle do jogador", new Color(220, 180, 0));

    private final String nome;
    private final Color cor;

    Comportamento(String nome, Color cor) {
        this.nome = nome;
        this.cor = cor;
    }

    public String getNome() {
        return nome;
    }

    public Color getCor() {
        return cor;
    }
}
