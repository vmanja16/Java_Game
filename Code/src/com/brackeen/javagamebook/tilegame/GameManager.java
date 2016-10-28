package com.brackeen.javagamebook.tilegame;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;

import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioFormat;

import com.brackeen.javagamebook.graphics.*;
import com.brackeen.javagamebook.sound.*;
import com.brackeen.javagamebook.input.*;
import com.brackeen.javagamebook.test.GameCore;
import com.brackeen.javagamebook.tilegame.sprites.*;

/**
    GameManager manages all parts of the game.
*/

// TODO: After 2 units of movement: grub shoots
    //   Shoot when pressing S, give it a sound
public class GameManager extends GameCore {

    public static void main(String[] args) {
        new GameManager().run();
    }

    // uncompressed, 44100Hz, 16-bit, mono, signed, little-endian
    private static final AudioFormat PLAYBACK_FORMAT =
        new AudioFormat(44100, 16, 1, true, false);

    private static final int DRUM_TRACK = 1;

    public static final float GRAVITY = 0.002f;

	public static final float ONE_SECOND = 1000;

    public static final float GRUB_SHOOTING_PERIOD = 3000;

    public static final float MOVEMENT_UNIT = 64;

    public static final float BULLET_RANGE = 10 * MOVEMENT_UNIT;

    public static final long COLLISION_LONG = -1;
    private Point pointCache = new Point();
    private TileMap map;
    private MidiPlayer midiPlayer;
    private SoundManager soundManager;
    private ResourceManager resourceManager;
    private Sound prizeSound;
    private Sound boopSound;
    private InputManager inputManager;
    private TileMapRenderer renderer;

    private GameAction moveLeft;
    private GameAction moveRight;
    private GameAction jump;
    private GameAction exit;
    
    // NEW VARIABLES!
    private GameAction shoot;
    public long shooting_time;

    public void init() {
        super.init();

        // set up input manager
        initInput();

        // start resource manager
        resourceManager = new ResourceManager(
        screen.getFullScreenWindow().getGraphicsConfiguration());

        // load resources
        renderer = new TileMapRenderer();
        renderer.setBackground(
         //   resourceManager.loadImage("background.png"));
		    resourceManager.loadImage("grey.png"));

        // load first map
        map = resourceManager.loadNextMap();

        // load sounds
        soundManager = new SoundManager(PLAYBACK_FORMAT);
        prizeSound = soundManager.getSound("sounds/prize.wav");
        boopSound = soundManager.getSound("sounds/boop2.wav");

        // start music
        midiPlayer = new MidiPlayer();
        Sequence sequence =
            midiPlayer.getSequence("sounds/music.midi");
        midiPlayer.play(sequence, true);
        toggleDrumPlayback();
    }


    /**
        Closes any resurces used by the GameManager.
    */
    public void stop() {
        super.stop();
        midiPlayer.close();
        soundManager.close();
    }


    private void initInput() {
        moveLeft = new GameAction("moveLeft");
        moveRight = new GameAction("moveRight");
        jump = new GameAction("jump",
            GameAction.DETECT_INITAL_PRESS_ONLY);
        exit = new GameAction("exit",
            GameAction.DETECT_INITAL_PRESS_ONLY);
		shoot = new GameAction("shoot");

        inputManager = new InputManager(
            screen.getFullScreenWindow());
        inputManager.setCursor(InputManager.INVISIBLE_CURSOR);

        inputManager.mapToKey(moveLeft, KeyEvent.VK_LEFT);
        inputManager.mapToKey(moveRight, KeyEvent.VK_RIGHT);
        inputManager.mapToKey(jump, KeyEvent.VK_UP);
        inputManager.mapToKey(exit, KeyEvent.VK_ESCAPE);
        inputManager.mapToKey(shoot, KeyEvent.VK_S);
	}

    
    private void checkInput(long elapsedTime) {

        if (exit.isPressed()) {
            stop();
        }

        Player player = (Player)map.getPlayer();
        if (player.isAlive()) {
            float velocityX = 0;
            if (moveLeft.isPressed()) {
                velocityX-=player.getMaxSpeed(); player.dir = false;
            }
            if (moveRight.isPressed()) {
                velocityX+=player.getMaxSpeed(); player.dir = true;
            }
            if (jump.isPressed()) {
                player.jump(false);
            }
            if (shoot.isPressed()){
                shooting_time += elapsedTime;
                if (shooting_time > GRUB_SHOOTING_PERIOD/2){
                    createPlayerBullet(player);
                    shooting_time = 0;
                }
            }
            else{shooting_time = 0;}
            player.setVelocityX(velocityX);
        }

    }

    private void createPlayerBullet(Player player){
        PlayerBullet bullet = (PlayerBullet)resourceManager.playerBulletSprite.clone();
        float pos_x = player.getX();
        if (player.dir){
            bullet.setX(pos_x+3*MOVEMENT_UNIT); bullet.flipMaxSpeed();
        }
        else{
            bullet.setX(pos_x-3*MOVEMENT_UNIT);
        }
        bullet.setY(player.getY());
        map.addSprite(bullet);
    }


    public void draw(Graphics2D g) {
        renderer.draw(g, map,
            screen.getWidth(), screen.getHeight());
    }


    /**
        Gets the current map.
    */
    public TileMap getMap() {
        return map;
    }


    /**
        Turns on/off drum playback in the midi music (track 1).
    */
    public void toggleDrumPlayback() {
        Sequencer sequencer = midiPlayer.getSequencer();
        if (sequencer != null) {
            sequencer.setTrackMute(DRUM_TRACK,
                !sequencer.getTrackMute(DRUM_TRACK));
        }
    }


    /**
        Gets the tile that a Sprites collides with. Only the
        Sprite's X or Y should be changed, not both. Returns null
        if no collision is detected.
    */
    public Point getTileCollision(Sprite sprite,
        float newX, float newY)
    {
        float fromX = Math.min(sprite.getX(), newX);
        float fromY = Math.min(sprite.getY(), newY);
        float toX = Math.max(sprite.getX(), newX);
        float toY = Math.max(sprite.getY(), newY);

        // get the tile locations
        int fromTileX = TileMapRenderer.pixelsToTiles(fromX);
        int fromTileY = TileMapRenderer.pixelsToTiles(fromY);
        int toTileX = TileMapRenderer.pixelsToTiles(
            toX + sprite.getWidth() - 1);
        int toTileY = TileMapRenderer.pixelsToTiles(
            toY + sprite.getHeight() - 1);

        // check each tile for a collision
        for (int x=fromTileX; x<=toTileX; x++) {
            for (int y=fromTileY; y<=toTileY; y++) {
                if (x < 0 || x >= map.getWidth() ||
                    map.getTile(x, y) != null)
                {
                    // collision found, return the tile
                    pointCache.setLocation(x, y);
                    return pointCache;
                }
            }
        }

        // no collision found
        return null;
    }


    /**
        Checks if two Sprites collide with one another. Returns
        false if the two Sprites are the same. Returns false if
        one of the Sprites is a Creature that is not alive.
    */
    public boolean isCollision(Sprite s1, Sprite s2) {
        // if the Sprites are the same, return false
        if (s1 == s2) {
            return false;
        }

        // if one of the Sprites is a dead Creature, return false
        if (s1 instanceof Creature && !((Creature)s1).isAlive()) {
            return false;
        }
        if (s2 instanceof Creature && !((Creature)s2).isAlive()) {
            return false;
        }

        // get the pixel location of the Sprites
        int s1x = Math.round(s1.getX());
        int s1y = Math.round(s1.getY());
        int s2x = Math.round(s2.getX());
        int s2y = Math.round(s2.getY());

        // check if the two sprites' boundaries intersect
        return (s1x < s2x + s2.getWidth() &&
            s2x < s1x + s1.getWidth() &&
            s1y < s2y + s2.getHeight() &&
            s2y < s1y + s1.getHeight());
    }


    /**
        Gets the Sprite that collides with the specified Sprite,
        or null if no Sprite collides with the specified Sprite.
    */
    public Sprite getSpriteCollision(Sprite sprite) {

        // run through the list of Sprites
        Iterator i = map.getSprites();
        while (i.hasNext()) {
            Sprite otherSprite = (Sprite)i.next();
            if (isCollision(sprite, otherSprite)) {
                // collision found, return the Sprite
                return otherSprite;
            }
        }

        // no collision found
        return null;
    }


    /**
        Updates Animation, position, and velocity of all Sprites
        in the current map.
    */
    public void update(long elapsedTime) {
        Creature player = (Creature)map.getPlayer();


        // player is dead! start map over
        if (player.getState() == Creature.STATE_DEAD) {
            map = resourceManager.reloadMap();
            return;
        }

        // get keyboard/mouse input
        checkInput(elapsedTime);

        // update player
        updateCreature(player, elapsedTime);
        player.update(elapsedTime);
		updateHealth(elapsedTime);
        // update other sprites
        ArrayList<GrubBullet> bulletQueue = new ArrayList<GrubBullet>();
        Iterator i = map.getSprites();
        while (i.hasNext()) {
            Sprite sprite = (Sprite)i.next();
            if (sprite instanceof Creature) {
                Creature creature = (Creature)sprite;
                if (creature.getState() == Creature.STATE_DEAD) {
                    i.remove();
                }
                else {
                    updateCreature(creature, elapsedTime);
                    if (creature instanceof Grub){
                        bulletQueue.add(makeGrubBullet(creature, elapsedTime));                        
                    }
                }
            }
            // normal update
            sprite.update(elapsedTime);
        }
        for (GrubBullet bullet : bulletQueue){
            if (bullet != null){map.addSprite(bullet);}
        }
    }
   /**
       Make GrubBullet if applicable
   */
    private GrubBullet makeGrubBullet(Creature creature, long elapsedTime){
        int mapWidth = TileMapRenderer.tilesToPixels(map.getWidth());
        Player player = (Player) map.getPlayer();
        int screenWidth = screen.getWidth();
        Grub grub = (Grub) creature;
        GrubBullet bullet = null;
        float grub_pos = grub.getX();
        float player_pos = player.getX();
        // get the scrolling position of the map
        // based on player's position
        int offsetX = screenWidth / 2 -
            Math.round(player_pos) - TileMapRenderer.TILE_SIZE;
        offsetX = Math.min(offsetX, 0);
        offsetX = Math.max(offsetX, screenWidth - mapWidth);
        int x_r = screenWidth - offsetX;
        int x_l = -offsetX;
        // Set grub.on_screen
        if ((grub_pos < x_r) && (grub_pos > x_l)){grub.setOnScreen(true);}
        else{grub.setOnScreen(false);}

        boolean facing_left = grub.isOnScreen() && (grub.getVelocityX() < 0)
                         && (grub_pos > player_pos);
        boolean facing_right = grub.isOnScreen() && (grub.getVelocityX() > 0)
                         && (grub_pos < player_pos);
        if (facing_left||facing_right){grub.wait_time += elapsedTime;
            if ( Math.abs(player_pos - grub.relative_position) > 2 *MOVEMENT_UNIT){}
        }
        else{grub.wait_time = 0; grub.relative_position = player_pos;}
        
        // set first_shot
        grub.first_shot = grub.allow_shooting;
        // check wait_time
        if ( (grub.wait_time > ONE_SECOND/2)){grub.allow_shooting = true;}
        else{grub.allow_shooting = false;}
        
        if (grub.allow_shooting){
            grub.shooting_time += elapsedTime;
            if ((grub.shooting_time > GRUB_SHOOTING_PERIOD) || (!grub.first_shot)){
                bullet = (GrubBullet)resourceManager.grubBulletSprite.clone();
                if(facing_left){bullet.setX(grub_pos-MOVEMENT_UNIT);}
                if(facing_right){bullet.setX(grub_pos+MOVEMENT_UNIT); bullet.flipMaxSpeed();}
                bullet.setY(grub.getY());
                grub.shooting_time = 0;
            }
        }
        else{grub.shooting_time = 0;}

        return bullet; 
    }
   /**
	   Updates the health
   */
    private void updateHealth(long elapsedTime){
        Player player = (Player)(map.getPlayer());
	    if (elapsedTime == COLLISION_LONG){
            if (map.getHealth() < 5){map.setHealth(0);}
            else{map.setHealth(map.getHealth()-5);}
        }
		else if (player.isAlive()){
            float x_pos = player.getX();
	        float pos_diff = Math.abs(x_pos-player.ref_pos);
			// update stall_time
			if(player.old_pos == x_pos){player.stall_time += elapsedTime;}
            else{player.stall_time = 0;}
            player.old_pos = x_pos;
			// update position
			if (pos_diff > MOVEMENT_UNIT) {
			   player.ref_pos = x_pos;
                if (map.getHealth().intValue() < 40){
                    map.setHealth(map.getHealth().intValue() + 1);
                }
            }
			// standing still for a full second
			else if (player.stall_time > ONE_SECOND){
			    player.stall_time = 0;
                if(map.getHealth() > 35){map.setHealth(40);}
				else{map.setHealth(map.getHealth()+5);}
			}
        }
		else{map.setHealth(0);}// Player dead!
    }

    /**
        Updates the creature, applying gravity for creatures that
        aren't flying, and checks collisions.
    */
    private void updateCreature(Creature creature,
        long elapsedTime)
    {

        // apply gravity
        if (!creature.isFlying()) {
            creature.setVelocityY(creature.getVelocityY() +
                GRAVITY * elapsedTime);
        }


        // change x
        float dx = creature.getVelocityX();
        float oldX = creature.getX();
        float move = dx * elapsedTime;
        float newX = oldX + move;

        if (creature instanceof GrubBullet){
            ((GrubBullet)creature).distance += Math.abs(move);
            if (((GrubBullet) creature).distance > BULLET_RANGE){creature.setState(Creature.STATE_DEAD);}
        }
        else if (creature instanceof PlayerBullet){
            ((PlayerBullet)creature).distance += Math.abs(move);
            if (((PlayerBullet) creature).distance > BULLET_RANGE){creature.setState(Creature.STATE_DEAD);}
        }


        Point tile =
            getTileCollision(creature, newX, creature.getY());
        if (tile == null) {
            creature.setX(newX);
        }
        else {
            // line up with the tile boundary
            if (dx > 0) {
                creature.setX(
                    TileMapRenderer.tilesToPixels(tile.x) -
                    creature.getWidth());
            }
            else if (dx < 0) {
                creature.setX(
                    TileMapRenderer.tilesToPixels(tile.x + 1));
            }
            creature.collideHorizontal();
        }
        if (creature instanceof Player) {
            checkPlayerCollision((Player)creature);
        }

        // change y
        float dy = creature.getVelocityY();
        float oldY = creature.getY();
        float newY = oldY + dy * elapsedTime;
        tile = getTileCollision(creature, creature.getX(), newY);
        if (tile == null) {
            creature.setY(newY);
        }
        else {
            // line up with the tile boundary
            if (dy > 0) {
                creature.setY(
                    TileMapRenderer.tilesToPixels(tile.y) -
                    creature.getHeight());
            }
            else if (dy < 0) {
                creature.setY(
                    TileMapRenderer.tilesToPixels(tile.y + 1));
            }
            creature.collideVertical();
        }
        if (creature instanceof Player) {
            checkPlayerCollision((Player)creature);
        }
        if (creature instanceof PlayerBullet){
            checkPlayerBulletCollision((PlayerBullet)creature);
        }


    }
    /**
        Checks for PlayerBullet collision with other Sprites.
    */
    public void checkPlayerBulletCollision(PlayerBullet bullet){
        Sprite collisionSprite = getSpriteCollision(bullet);
        if (collisionSprite instanceof Grub){
            ((Creature)collisionSprite).setState(Creature.STATE_DYING);
            // MAKE A SOUND!
        }
    }

    /**
        Checks for Player collision with other Sprites. If
        canKill is true, collisions with Creatures will kill
        them.
    */
    public void checkPlayerCollision(Player player)
    {
        if (!player.isAlive()) {
            return;
        }

        // check for player collision with other sprites
        Sprite collisionSprite = getSpriteCollision(player);
        if (collisionSprite instanceof PowerUp) {
            acquirePowerUp((PowerUp)collisionSprite);
        }
        else if (collisionSprite instanceof Creature) {
        	// player dies!
            if (collisionSprite instanceof GrubBullet){
                updateHealth(COLLISION_LONG);
                player.stall_time = 0; // dont want to imm gain 5 hp
                ((Creature)collisionSprite).setState(Creature.STATE_DEAD);
                if (map.getHealth()==0 ){
                    player.setState(Creature.STATE_DYING);
                    updateHealth(0);
                }

            }
        	if (collisionSprite instanceof Grub) {
                player.setState(Creature.STATE_DYING);
			    updateHealth(0);
            }
        }
    }


    /**
        Gives the player the speicifed power up and removes it
        from the map.
    */
    public void acquirePowerUp(PowerUp powerUp) {
        // remove it from the map
        map.removeSprite(powerUp);

        if (powerUp instanceof PowerUp.Star) {
            // do something here, like give the player points
            soundManager.play(prizeSound);
        }
        else if (powerUp instanceof PowerUp.Music) {
            // change the music
            soundManager.play(prizeSound);
            toggleDrumPlayback();
        }
        else if (powerUp instanceof PowerUp.Goal) {
            // advance to next map
            soundManager.play(prizeSound,
                new EchoFilter(2000, .7f), false);
            map = resourceManager.loadNextMap();
        }
    }

}
