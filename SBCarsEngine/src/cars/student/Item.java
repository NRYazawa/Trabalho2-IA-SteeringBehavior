package cars.student;

import cars.engine.Vector2;

/** Um loot espalhado pelo escritório. */
public class Item {

    private static final String[] NOMES = {
            "Kit médico", "Munição", "Lanterna", "Enlatado", "Bateria", "Garrafa d'água", "Rádio", "Cabo HDMI"
    };

    private final Vector2 posicao;
    private final String nome;

    public Item(Vector2 posicao) {
        this.posicao = posicao.clone();
        this.nome = NOMES[(int) (Math.random() * NOMES.length)];
    }

    public Vector2 getPosicao() {
        return posicao.clone();
    }

    public String getNome() {
        return nome;
    }
}
