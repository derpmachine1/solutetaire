package com.github.solutetaire;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;

public class GameScreen implements Screen{
    final SoluteTaire game;
    private int timeElapsed;
    private int animationTimeElapsed;
    private int animationTime;

    private OrthographicCamera camera;
    private Texture cardSpaceImage;
    private Texture cardBackImage;
    private Texture spadeImage;
    private Texture heartImage;
    private Texture clubImage;
    private Texture diamondImage;
    private Texture cardSpaceSpadeImage;
    private Texture cardSpaceHeartImage;
    private Texture cardSpaceClubImage;
    private Texture cardSpaceDiamondImage;

    // Stores cards
    private CardCollection stock;
    private CardCollection waste;
    private Foundation[] foundations;
    private CardCollection[] tableau;
    private CardCollection hand;  // Cards being dragged around by the mouse

    private char[] handOrigin = new char[2];
    private float[] handOriginDimensions = new float[4];
    private int[] oldFoundationSizes = new int[4];

    public GameScreen(final SoluteTaire game) {
        this.game = game;
        timeElapsed = 0;
        animationTimeElapsed = 0;
        animationTime = 25;

        // Creates and sets camera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 1280, 720);

        // Creates images
        cardSpaceImage = new Texture(Gdx.files.internal("cardSpace.png"));
        cardBackImage = new Texture(Gdx.files.internal("cardBack.png"));
        spadeImage = new Texture(Gdx.files.internal("spade.png"));
        heartImage = new Texture(Gdx.files.internal("heart.png"));
        clubImage = new Texture(Gdx.files.internal("club.png"));
        diamondImage = new Texture(Gdx.files.internal("diamond.png"));
        cardSpaceSpadeImage = new Texture(Gdx.files.internal("cardSpaceSpade.png"));
        cardSpaceHeartImage = new Texture(Gdx.files.internal("cardSpaceHeart.png"));
        cardSpaceClubImage = new Texture(Gdx.files.internal("cardSpaceClub.png"));
        cardSpaceDiamondImage = new Texture(Gdx.files.internal("cardSpaceDiamond.png"));

        // Sets up all the stacks of cards
        stock = new CardCollection(true);
        waste = new CardCollection(false);
        foundations = new Foundation[4];
        foundations[0] = new Foundation('s');
        foundations[1] = new Foundation('h');
        foundations[2] = new Foundation('c');
        foundations[3] = new Foundation('d');
        tableau = new CardCollection[7];
        for (int i = 0; i < 7; i++) {
            tableau[i] = new CardCollection();
        }
        hand = new CardCollection(false);

        stock.shuffle();

        // Adds cards to tableau
        for (int i = 0; i < 7; i++) {
            for (int j = i; j < 7; j++) {
                tableau[j].addCard(stock.popLastCard());
            }
        }

        /*
        tableau[0].addCard(new Card('s', 1));
        tableau[1].addCard(new Card('d', 1));
        tableau[2].addCard(new Card('c', 1));
        tableau[3].addCard(new Card('h', 1));
        */

        // Flips last cards in tableau
        for (int i = 0; i < 7; i++) {
            tableau[i].getLastCard().flip();
        }
    }

    @Override
    public void render(float delta) {
        timeElapsed++;
        game.ui.offsetDimensions(timeElapsed);
        
        if (game.timeSinceClick < game.clickDelay) {
            game.timeSinceClick++;
        }
        if (animationTimeElapsed < animationTime) {
            animationTimeElapsed++;
        } else {
            for (int i = 0; i < 4; ++i) {
                oldFoundationSizes[i] = foundations[i].getSize();
            }
        }

        // If clicked, gets mouse position and checks for other actions
        if (Gdx.input.isTouched()) {
            game.mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(game.mouse);

            // If clicking on stock
            if (game.isInside(game.mouse.x, game.mouse.y, game.ui.getStock()) & game.timeSinceClick >= game.clickDelay) {
                // If stock is empty, take everything in waste, reverse it, and move it to stock
                if (stock.getSize() == 0) {
                    waste.reverse();
                    stock.setCards(waste.getCards());
                    waste.clear();
                    stock.flipAll();
                    // If stock is not empty, move the top card to waste
                } else {
                    waste.addCard(stock.popLastCard());
                    waste.getLastCard().flip();
                }
            }

            // If clicking on waste, waste is not empty, and user is not holding any cards
            if (game.isInside(game.mouse.x, game.mouse.y, game.ui.getWaste()) & waste.getSize() > 0 & hand.getSize() == 0) {
                hand.addCard(waste.popLastCard());
                handOrigin[0] = 'w';
                handOriginDimensions = game.ui.getWaste();
                animationTimeElapsed = 0;
            }

            // If clicking on foundation, foundation is not empty, and user is not holding any cards
            for (int i = 0; i < 4; i++) {
                if (game.isInside(game.mouse.x, game.mouse.y, game.ui.getFoundations(i)) & foundations[i].getSize() > 0 & hand.getSize() == 0) {
                    hand.addCard(foundations[i].popLastCard());
                    handOrigin[0] = 'f';
                    handOrigin[1] = (char) i;
                    handOriginDimensions = game.ui.getFoundations(i);
                    animationTimeElapsed = 0;
                }
            }

            // If clicking on tableau, tableau is not empty, user is not holding any cards, and tableau card is face up
            for (int i = 0; i < 7; i++) {
                // For loop is reversed so cards on top are prioritized
                for (int j = tableau[i].getSize() - 1; j >= 0; j--) {
                    if (game.isInside(game.mouse.x, game.mouse.y, game.ui.getTableau(i, j)) & tableau[i].getSize() > 0 & hand.getSize() == 0 & tableau[i].getCard(j).isFaceUp()) {
                        hand.addCards(tableau[i].getCards(j));
                        tableau[i].clear(j);
                        handOrigin[0] = 't';
                        handOrigin[1] = (char) i;
                        handOriginDimensions = game.ui.getTableau(i, j);
                        animationTimeElapsed = 0;
                    }
                }
            }

            if (game.timeSinceClick >= game.clickDelay) {
                game.timeSinceClick = 0;
            }

            // If not clicked
        } else {
            // If hand is not empty, tries to empty it
            while (hand.getSize() > 0) {
                // Tries to place cards in foundation
                for (int i = 0; i < 4; i++) {
                    // If hovering over foundation and user is only holding one card
                    if (game.isInside(game.mouse.x, game.mouse.y, game.ui.getFoundations(i)) & hand.getSize() == 1) {
                        // If foundation is empty
                        if (foundations[i].getSize() == 0) {
                            // If card matches suit of foundation and is an ace
                            if (hand.getLastCard().getSuit() == foundations[i].getSuit() & hand.getLastCard().getRank() == 1) {
                                foundations[i].addCard(hand.popLastCard());
                                animationTimeElapsed = 0;
                            }
                            // If foundation is not empty
                        } else {
                            // If card matches suit of foundation and is one bigger
                            if (hand.getLastCard().getSuit() == foundations[i].getSuit() & hand.getLastCard().getRank() == foundations[i].getLastCard().getRank() + 1) {
                                foundations[i].addCard(hand.popLastCard());
                                animationTimeElapsed = 0;
                            }
                        }
                    }
                    if (hand.getSize() == 0) {
                        break;
                    }
                }
                if (hand.getSize() == 0) {
                    break;
                }

                // Tries to place cards in tableau
                for (int i = 0; i < 7; i++) {
                    // If hovering over last card in tableau
                    if (game.isInside(game.mouse.x, game.mouse.y, game.ui.getTableau(i, tableau[i].getSize() - 1))) {
                        // If tableau is empty
                        if (tableau[i].getSize() == 0) {
                            // If card is a king
                            if (hand.getCard(0).getRank() == 13) {
                                tableau[i].setCards(hand.getCards());
                                hand.clear();
                                animationTimeElapsed = 0;
                            }
                            // If tableau is not empty
                        } else {
                            // If first card in hand is opposite suit colour of last card in tableau and is one smaller
                            if (isOppositeColour(hand.getCard(0), tableau[i].getLastCard()) & hand.getCard(0).getRank() == tableau[i].getLastCard().getRank() - 1) {
                                tableau[i].addCards(hand.getCards());
                                hand.clear();
                                animationTimeElapsed = 0;
                            }
                        }
                    }
                    if (hand.getSize() == 0) {
                        break;
                    }
                }
                if (hand.getSize() == 0) {
                    break;
                }

                // If all else fails, places cards back to where they originated from
                switch (handOrigin[0]) {
                    case 'w':
                        waste.addCard(hand.popLastCard());
                        animationTimeElapsed = 0;
                        break;
                    case 'f':
                        foundations[(int) handOrigin[1]].addCard(hand.popLastCard());
                        animationTimeElapsed = 0;
                        break;
                    case 't':
                        tableau[(int) handOrigin[1]].addCards(hand.getCards());
                        hand.clear();
                        animationTimeElapsed = 0;
                        break;
                }
            }
        }

        // Clears screen
        Gdx.gl.glClearColor(0.4f, 0.2f, 0.6f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Updates camera
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.shape.setProjectionMatrix(camera.combined);

        // Draws background visuals
        game.shape.begin(ShapeRenderer.ShapeType.Filled);

        // Draws pouring animation
        if (foundations[0].getSize() == oldFoundationSizes[0] + 1) {
            game.shape.setColor(new Color(0.5f, 0.25f, 1f, 1f));
            game.shape.rect(game.ui.getScreenW() * 7 / 15, 0, game.ui.getScreenW() / 15, game.ui.getScreenH());
        }

        // Calculates a modifier to how pink the solution will be based on amount of water and indicator
        float pinkModifier;
        if (foundations[0].getSize() == 0) {
            pinkModifier = 0;
        } else {
            pinkModifier = ((float) foundations[1].getSize()) / ((float) foundations[0].getSize());
        }

        // If no base or acid, make solution blue
        if (foundations[2].getSize() + foundations[3].getSize() == 0) {
            game.shape.setColor(new Color(0.5f, 0.25f, 1f, 1f));
        // If more acid than base, make solution blue
        } else if (foundations[2].getSize() < foundations[3].getSize()) {
            game.shape.setColor(new Color(0.5f, 0.25f, 1f, 1f));
        // If equal base and acid, make solution slightly pink
        } else if (foundations[2].getSize() == foundations[3].getSize()) {
            game.shape.setColor(new Color(0.5f + 0.25f * pinkModifier, 0.25f, 1f, 1f));
        // If more base than acid, make solution pink
        } else if (foundations[2].getSize() > foundations[3].getSize()) {
            game.shape.setColor(new Color(0.5f + 0.5f * pinkModifier, 0.25f, 1f, 1f));
        }
        game.shape.rect(0, 0, game.ui.getScreenW(), game.ui.getScreenH() * (foundations[0].getSize() - (1 - (float) animationTimeElapsed / animationTime) * (foundations[0].getSize() - oldFoundationSizes[0])) / 13);

        game.shape.end();

        // Draws foreground visuals
        game.batch.begin();

        // Draws stock
        if (stock.getSize() == 0) {
            game.draw(game.ui.getStock(), cardSpaceImage);
        } else {
            drawCard(game.ui.getStock(), stock.getLastCard());
        }

        // Draws waste
        if (waste.getSize() == 0) {
            game.draw(game.ui.getWaste(), cardSpaceImage);
        } else {
            drawCard(game.ui.getWaste(), waste.getLastCard());
        }

        // Draws foundations
        for (int i = 0; i < 4; i++) {
            if (foundations[i].getSize() == 0) {
                switch (i) {
                    case 0:
                        game.draw(game.ui.getFoundations(i), cardSpaceSpadeImage);
                        break;
                    case 1:
                        game.draw(game.ui.getFoundations(i), cardSpaceHeartImage);
                        break;
                    case 2:
                        game.draw(game.ui.getFoundations(i), cardSpaceClubImage);
                        break;
                    case 3:
                        game.draw(game.ui.getFoundations(i), cardSpaceDiamondImage);
                        break;
                }

            } else {
                drawCard(game.ui.getFoundations(i), foundations[i].getLastCard());
            }
        }

        // Draws tableau
        for (int i = 0; i < 7; i++) {
            if (tableau[i].getSize() == 0) {
                game.draw(game.ui.getTableau(i), cardSpaceImage);
            } else {
                // Makes sure that the last card is face up if user is not holding anything
                if (!tableau[i].getLastCard().isFaceUp() & hand.getSize() == 0) {
                    tableau[i].getLastCard().flip();
                }

                for (int j = 0; j < tableau[i].getSize(); j++) {
                    drawCard(game.ui.getTableau(i, j), tableau[i].getCard(j));
                }
            }
        }

        // Draws cards in hand
        if (hand.getSize() > 0) {
            for (int i = 0; i < hand.getSize(); i++) {
                float x = handOriginDimensions[0] + handOriginDimensions[2] / 2 + ((float) Math.min(animationTimeElapsed, animationTime / 5) / (animationTime / 5f)) * (game.mouse.x - handOriginDimensions[0] - handOriginDimensions[2] / 2);
                float y = handOriginDimensions[1] + handOriginDimensions[3] / 2 + ((float) Math.min(animationTimeElapsed, animationTime / 5) / (animationTime / 5f)) * (game.mouse.y - handOriginDimensions[1] - handOriginDimensions[3] / 2);
                drawCard(game.ui.getCardDimensions(x, y, i), hand.getCard(i));
            }
        }

        game.batch.end();

        // Checks if game is over
        boolean victory = true;
        for (int i = 0; i < 7; i++) {
            if (tableau[i].getSize() > 0) {
                victory = false;
            }
        }
        if (victory) {
            game.setEndScreen();
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        cardSpaceImage.dispose();
        cardBackImage.dispose();
        heartImage.dispose();
        diamondImage.dispose();
        spadeImage.dispose();
        clubImage.dispose();
        cardSpaceSpadeImage.dispose();
        cardSpaceHeartImage.dispose();
        cardSpaceClubImage.dispose();
        cardSpaceDiamondImage.dispose();
    }

    // Draws cards with given location, dimensions, suit, and card
    public void drawCard(float x, float y, float w, float h, Card card) {
        // If face up
        if (card.isFaceUp()) {
            // Draws card background with suit
            switch (card.getSuit()) {
                case 's':
                    game.batch.draw(spadeImage, x, y, w, h);
                    break;
                case 'h':
                    game.batch.draw(heartImage, x, y, w, h);
                    break;
                case 'c':
                    game.batch.draw(clubImage, x, y, w, h);
                    break;
                case 'd':
                    game.batch.draw(diamondImage, x, y, w, h);
                    break;
            }
            // Draws rank on top
            switch (card.getRank()) {
                case 1:
                    game.fontSmall.draw(game.batch, "A", x + w / 20, y + h * 19 / 20);
                    break;
                case 11:
                    game.fontSmall.draw(game.batch, "J", x + w / 20, y + h * 19 / 20);
                    break;
                case 12:
                    game.fontSmall.draw(game.batch, "Q", x + w / 20, y + h * 19 / 20);
                    break;
                case 13:
                    game.fontSmall.draw(game.batch, "K", x + w / 20, y + h * 19 / 20);
                    break;
                default:
                    game.fontSmall.draw(game.batch, String.valueOf(card.getRank()), x + w / 20, y + h * 19 / 20);
                    break;
            }
            // If face down
        } else {
            game.batch.draw(cardBackImage, x, y, w, h);
        }
    }

    // Same as the original method but overloaded so the dimensions come from a single array
    public void drawCard(float[] dimensions, Card card) {
        drawCard(dimensions[0], dimensions[1], dimensions[2], dimensions[3], card);
    }

    // Checks if two cards are opposite colours
    public boolean isOppositeColour(Card a, Card b) {
        if (a.getSuit() == 's' | a.getSuit() == 'c') {
            if (b.getSuit() == 'h' | b.getSuit() == 'd') {
                return true;
            } else {
                return false;
            }
        } else {
            if (b.getSuit() == 's' | b.getSuit() == 'c') {
                return true;
            } else {
                return false;
            }
        }
    }
}
