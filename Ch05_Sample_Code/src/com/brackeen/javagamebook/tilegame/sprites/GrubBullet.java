package com.brackeen.javagamebook.tilegame.sprites;

import com.brackeen.javagamebook.graphics.Animation;

/**
    A GrubBullet  is a bullet shot by a Grub.
*/
public class GrubBullet extends Creature {

	private float max_speed;

	public float distance;

    public GrubBullet(Animation left, Animation right,
        Animation deadLeft, Animation deadRight)
    {
        super(left, right, deadLeft, deadRight);
        max_speed = 0.08f;
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

}
