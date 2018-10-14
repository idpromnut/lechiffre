package org.unrecoverable.lechiffre.commands;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.util.Log;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.impl.obj.PrivateChannel;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

@Slf4j
public class DiceCommand extends BaseCommand implements ICommand, ImageObserver {

	//private static final Resource diceSourceImage = new ClassPathResource("dice-face-clipart.png");
	private static final Resource diceSourceImage = new ClassPathResource("fancydice.png");
	
	private BufferedImage[] d6 = null;
	
	public DiceCommand() {
		loadDiceSourceImages();
	}

	@Override
	public String getCommand() {
		return Commands.CMD_ROLL;
	}

	@Override
	public String getHelp() {
		return "try rolling some dice, see where it gets you... (/roll 1d4, /roll d6, etc)";
	}

	@Override
	public boolean isGuildCommand() {
		return false;
	}

	@Override
	public Pair<BotReply, String> handle(IMessage message) {
		
		final String content = message.getContent();
		final IChannel channel = message.getChannel();
		final IUser author = message.getAuthor();
		final String diceCommand = StringUtils.substring(content, StringUtils.indexOf(content, " "));
		Collection<Die> diceToRoll = createFromDieScriptor(diceCommand);
		
//		StringBuilder rollResults = new StringBuilder();
//		for(Die die: diceToRoll) {
//			rollResults.append("D").append(die.getFaces()).append(" -> ").append(die.roll()).append("; ");
//		}

		replyWithDiceImage(createDiceImage(diceToRoll), channel, author);
		
		return Pair.of(BotReply.NONE, null);
	}
	
	
	private BufferedImage createDiceImage(final Collection<Die> diceToRoll) {
//		int width = 5 + (diceToRoll.size() * 22) + 4;
//		int height = 21;
//		int xPointer = 6;
//		int yPointer = 1;
		int width = 5 + (diceToRoll.size() * 101) + 4;
		int height = 106;
		int xPointer = 6;
		int yPointer = 1;
		BufferedImage responseImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = responseImage.createGraphics();
		graphics.setComposite(AlphaComposite.Clear);
		graphics.fillRect(0, 0, width, height);
		graphics.setComposite(AlphaComposite.SrcOver);
		for(Die dieToRoll: diceToRoll) {
			graphics.drawImage(d6[dieToRoll.roll() - 1], xPointer, yPointer, (ImageObserver)this);
			xPointer = xPointer + 101;
		}
		return responseImage;
	}
	
	private void replyWithDiceImage(final BufferedImage rolledDice, final IChannel channel, final IUser author) {
		try {
			ByteArrayOutputStream imageBaos = new ByteArrayOutputStream();
			ImageIO.write(rolledDice, "png", imageBaos);
			ByteArrayInputStream imageBais = new ByteArrayInputStream(imageBaos.toByteArray());
			IGuild guild = (channel instanceof PrivateChannel) ? null : channel.getGuild();
			channel.sendFile("Rolled dice for " + getNickname(author, guild) , imageBais, "rolledDiceImage.png");
		} catch (IOException | MissingPermissionsException | RateLimitException | DiscordException e ) {
			log.error("could not generate dice image", e);
		}
	}
	

	public Collection<Die> createFromDieScriptor(final String desc) {
		List<Die> dice = new ArrayList<>();
		String[] diceDescriptions = StringUtils.split(desc, " ");
		for(String rollDefinition: diceDescriptions) {
			String[] die = StringUtils.split(rollDefinition, "dD");
			if (die.length == 1) {
				dice.add(new Die(Integer.parseInt(die[0])));
			}
			else {
				for(int i = 0; i < Integer.parseInt(die[0]); i++) dice.add(new Die(Integer.parseInt(die[1])));
			}
		}
		return dice;
	}

	private void loadDiceSourceImages() {
		BufferedImage img = null;
		try {
		    img = ImageIO.read(diceSourceImage.getInputStream());
//		    d6 = new BufferedImage[6];
//		    d6[0] = img.getSubimage(2, 3, 21, 21);
//		    d6[1] = img.getSubimage(26, 3, 21, 21);
//		    d6[2] = img.getSubimage(51, 3, 21, 21);
//		    d6[3] = img.getSubimage(75, 3, 21, 21);
//		    d6[4] = img.getSubimage(99, 3, 21, 21);
//		    d6[5] = img.getSubimage(123, 3, 21, 21);

		    d6 = new BufferedImage[6];
		    d6[0] = img.getSubimage(6, 4, 101, 101);
		    d6[1] = img.getSubimage(111, 4, 101, 101);
		    d6[2] = img.getSubimage(217, 4, 101, 101);
		    d6[3] = img.getSubimage(322, 4, 101, 101);
		} catch (IOException e) {
			Log.error("could not load dice source image from disk", e);
		}
	}
	
	class Die {
		@Getter
		private int faces;
		
		public Die(int faces) {
			this.faces = faces;
		}
		
		public int roll() {
			return (int)((Math.random() * faces) + 1);
		}
	}

	@Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
		return true;
	}
}
