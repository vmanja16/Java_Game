package com.brackeen.javagamebook.tilegame.sprites;

import com.brackeen.javagamebook.graphics.Animation;

/**
    A Grub is a Creature that moves slowly on the ground.
*/
public class Grub extends Creature {

    private boolean on_screen;

    public boolean allow_shooting;

    public float shooting_time;

  	public float wait_time;

    public boolean first_shot;

    public Grub(Animation left, Animation right,
        Animation deadLeft, Animation deadRight)
    {
        super(left, right, deadLeft, deadRight);
        on_screen = false;
        wait_time = 0;
    }



    public boolean isOnScreen(){return on_screen;}

    public void setOnScreen(boolean value){
    	on_screen = value;
    }

    public float getMaxSpeed() {
        return 0.03f;
    }

}
