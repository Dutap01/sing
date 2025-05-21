package org.github.Dutap01.bot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.entities.Activity;
import org.github.Dutap01.bot.music.PlayerManager;
import org.github.Dutap01.bot.response.ChattingReaction;
import org.github.Dutap01.bot.response.SlashCommandReaction;
import org.github.Dutap01.bot.response.MusicButtonInteraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordBot {

    private static final Logger logger = LoggerFactory.getLogger(DiscordBot.class);

    public static void main(String[] args) {
        String token = System.getenv("DISCORD_BOT_TOKEN");

        if (token == null || token.trim().isEmpty()) {
            logger.error("오류: DISCORD_BOT_TOKEN 환경 변수가 설정되지 않았거나 비어 있습니다. 애플리케이션을 종료합니다.");
            System.exit(1);
            return;
        }

        try {
            JDABuilder builder = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_VOICE_STATES);
            builder.addEventListeners(
                    new ChattingReaction(),
                    new SlashCommandReaction(),
                    new MusicButtonInteraction()
            );

            builder.setActivity(Activity.playing("티타임"));

            net.dv8tion.jda.api.JDA jda = builder.build();

            jda.awaitReady();

            PlayerManager.getINSTANCE();
            logger.info("Discord 봇이 성공적으로 로그인되었고 로딩이 완료되었습니다!");

        } catch (IllegalArgumentException e) {
            logger.error("잘못된 토큰이 제공되었거나 필수 인텐트가 누락되었습니다: {}", e.getMessage());
        } catch (InterruptedException e) {
            logger.error("봇 시작 중 스레드 인터럽트가 발생했습니다: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
        catch (Exception e) {
            logger.error("봇을 시작하는 동안 알 수 없는 오류가 발생했습니다: {}", e.getMessage(), e);
        }
    }
}