package com.brackeen.javagamebook.tilegame.sprites;

import com.brackeen.javagamebook.graphics.Animation;

/**
    A PlayerBullet  is a bullet shot by the Player!.
*/
public class PlayerBullet extends Creature {

	private float max_speed;

	public float distance;

    public PlayerBullet(Animation left, Animation right,
        Animation deadLeft, Animation deadRight)
    {
        super(left, right, deadLeft, deadRight);
        max_speed = 0.16f;
    }


    // Destroys Bullet upon any non-creature collision!
    @Override
    public void collideHorizontal() {
        setState(STATE_DEAD);
    }

    public void flipMaxSpeed(){
    	max_speed = -max_speed;
    }

    public float getMaxSpeed() {
        return max_speed;
    }

    public boolean isFlying() {
        return isAlive();
    }


}
