# FpsBoost (Forge 1.8.9)

Menu: tecla **K** (mude em Controles) ou comando **/fpsboost** (ou /fps). Clique esquerdo avança a opção, direito volta.
Configuração salva em `config/fpsboost.cfg` (dá para mudar `targetFps`, padrão 60).

## Como compilar
1. Opção celular: suba esta pasta num repositório do GitHub -> aba Actions -> "Build" -> baixe o artefato FpsBoost-jar.
2. Opção PC: JDK 8 + MDK do Forge 1.8.9 (11.15.1.2318). Troque o `build.gradle` e a pasta `src` do MDK pelos daqui e rode `gradlew build`. O .jar sai em `build/libs`.

## Observações (o que é aproximação)
- Fast Math: só tem efeito se o OptiFine estiver instalado (o Forge puro já usa tabela de seno).
- Chunk Update Control: faz a Dynamic Render Distance mudar 1 chunk por vez com intervalo; o agendador interno de chunks do jogo não é alterado.
- Smart Animations: atualiza as texturas animadas a cada 2 ticks (3 no perfil Low), não só as visíveis.
- HUD Caching: reaproveita o HUD do próprio mod (4x/s); não cacheia o HUD do jogo.
- VBO: começa desligado, pode bugar em alguns celulares/launchers.
