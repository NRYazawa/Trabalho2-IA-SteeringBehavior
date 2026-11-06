package cars.student;

import cars.engine.Vector2;

import static cars.engine.Vector2.*;

/** Um projétil que anda em linha reta até acertar algo ou acabar o alcance. */
public class Tiro {

    private static final double VELOCIDADE = 450;
    private static final double TEMPO_DE_VIDA = 0.6; // segundos (~270 px de alcance)

    private final Vector2 posicao;
    private final Vector2 velocidade;
    private double tempoRestante = TEMPO_DE_VIDA;
    private boolean ativo = true;

    public Tiro(Vector2 origem, Vector2 alvo) {
        this.posicao = origem.clone();
        this.velocidade = multiply(normalize(subtract(alvo, origem)), VELOCIDADE);
    }

    public void mover(double secs) {
        posicao.add(multiply(velocidade, secs));
        tempoRestante -= secs;
        if (tempoRestante <= 0) ativo = false;
    }

    public void desativar() {
        ativo = false;
    }

    public boolean estaAtivo() { return ativo; }
    public Vector2 getPosicao() { return posicao.clone(); }
    public Vector2 getVelocidade() { return velocidade.clone(); }
}
