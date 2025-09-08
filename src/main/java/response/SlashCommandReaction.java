package org.github.Dutap01.bot.response;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import org.github.Dutap01.bot.music.PlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class SlashCommandReaction extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping":
                event.reply("Pong!").queue();
                break;
            case "reply":
                event.reply("Reply!").queue();
                break;
            case "join":
                joinVoiceChannelSlash(event);
                break;
            case "pause":
                pauseMusicSlash(event);
                break;
            case "skip":
                skipMusicSlash(event);
                break;
            case "list":
                displayQueueSlash(event);
                break;
            case "play":
                String songTitle = event.getOption("title").getAsString();
                playMusicSlash(event, songTitle);
                break;
            case "promote":
                if (event.getMember() == null || event.getMember().getVoiceState() == null || !event.getMember().getVoiceState().inAudioChannel() || !event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
                    event.reply("음악을 재생 할려면 먼저 보이스 채널에 접속하고 봇이 연결되어 있어야 합니다.").setEphemeral(true).queue();
                    return;
                }
                OptionMapping indexOption = event.getOption("index");
                if (indexOption == null) {
                    event.reply("재생 할 곡의 번호를 입력해주세요.").setEphemeral(true).queue();
                    return;
                }
                int trackIndex = indexOption.getAsInt();

                PlayerManager playerManager = PlayerManager.getINSTANCE();
                if (playerManager.promoteTrack(event.getGuild(), trackIndex)) {
                    event.reply(trackIndex + "번 곡을 다음 곡으로 끌어올렸습니다!").queue();
                    displayNowPlayingSlash(event);
                } else {
                    event.reply("유효하지 않은 곡 번호이거나 큐가 비어 있습니다.").setEphemeral(true).queue();
                }
                break;
        }
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandDatas = new ArrayList<>();
        commandDatas.add(Commands.slash("ping", "Pong을 해줍니다."));
        commandDatas.add(Commands.slash("reply", "Reply를 해줍니다."));
        commandDatas.add(Commands.slash("join", "봇을 보이스 채널에 참여시킵니다."));
        commandDatas.add(Commands.slash("pause", "현재 재생중인 음악을 일시정지합니다."));
        commandDatas.add(Commands.slash("skip", "현재 재생중인 음악을 스킵합니다."));
        commandDatas.add(Commands.slash("list", "현재 재생 목록을 표시합니다."));
        commandDatas.add(Commands.slash("play", "음악을 재생합니다.")
                .addOption(OptionType.STRING, "title", "재생할 음악 제목", true));
        commandDatas.add(Commands.slash("promote", "재생 목록의 특정 곡을 다음 곡으로 끌어올립니다.")
                .addOption(OptionType.INTEGER, "index", "끌어올릴 곡의 재생 목록 번호", true));


        event.getGuild().updateCommands().addCommands(commandDatas).queue();
    }

    private void joinVoiceChannelSlash(SlashCommandInteractionEvent event) {
        if (event.getMember() == null || event.getMember().getVoiceState() == null || !event.getMember().getVoiceState().inAudioChannel()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("보이스 채널 입장 오류");
            builder.setDescription("먼저 보이스 채널에 접속해야 합니다.");
            event.replyEmbeds(builder.build()).queue();
            return;
        }

        AudioManager audioManager = event.getGuild().getAudioManager();
        if (audioManager.isConnected()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("보이스 채널 입장 오류");
            builder.setDescription("봇이 이미 다른 보이스 채널에 연결되어 있습니다.");
            event.replyEmbeds(builder.build()).queue();
            return;
        }

        AudioChannel userChannel = event.getMember().getVoiceState().getChannel();
        if (userChannel instanceof VoiceChannel) {
            audioManager.openAudioConnection((VoiceChannel) userChannel);
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.GREEN);
            builder.setTitle("보이스 채널 입장 성공");
            builder.setDescription("`" + userChannel.getName() + "` 채널에 연결되었습니다.");
            event.replyEmbeds(builder.build()).queue();
        } else {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("보이스 채널 입장 오류");
            builder.setDescription("유효한 보이스 채널이 아닙니다.");
            event.replyEmbeds(builder.build()).queue();
        }
    }

    private void playMusicSlash(SlashCommandInteractionEvent event, String text) {
        event.deferReply(false).queue(hook -> {
            if (event.getMember() == null || event.getMember().getVoiceState() == null || !event.getMember().getVoiceState().inAudioChannel()) {
                hook.editOriginalEmbeds(new EmbedBuilder().setColor(Color.RED).setTitle("음악 재생 오류").setDescription("음악을 재생하려면 보이스 채널에 소속되어 있어야 합니다.").build()).queue();
                return;
            }

            if (event.getGuild().getSelfMember() == null || event.getGuild().getSelfMember().getVoiceState() == null || !event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
                final AudioManager audioManager = event.getGuild().getAudioManager();
                final AudioChannel memberChannel = event.getMember().getVoiceState().getChannel();

                if (memberChannel instanceof VoiceChannel) {
                    audioManager.openAudioConnection((VoiceChannel) memberChannel);
                } else {
                    hook.editOriginalEmbeds(new EmbedBuilder().setColor(Color.RED).setTitle("음악 재생 오류").setDescription("연결할 수 있는 유효한 보이스 채널이 아닙니다. (예: 스테이지 채널)").build()).queue();
                    return;
                }
            }

            String link = "ytsearch: " + text;
            PlayerManager.getINSTANCE().loadAndPlaySlash(hook, link, event.getMember());
        });
    }

    private void pauseMusicSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        if (event.getGuild().getSelfMember() == null || event.getGuild().getSelfMember().getVoiceState() == null || !event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("일시정지 오류");
            builder.setDescription("봇이 보이스 채널에 연결되어 있지 않습니다.");
            event.replyEmbeds(builder.build()).queue();
            return;
        }

        PlayerManager.getINSTANCE().pauseTrack(event.getGuild());
        displayNowPlayingSlash(event);
    }

    private void skipMusicSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        if (event.getGuild().getSelfMember() == null || event.getGuild().getSelfMember().getVoiceState() == null || !event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("스킵 오류");
            builder.setDescription("봇이 보이스 채널에 연결되어 있지 않습니다.");
            event.replyEmbeds(builder.build()).queue();
            return;
        }
        PlayerManager.getINSTANCE().skipTrack(event.getGuild());
        displayNowPlayingSlash(event);
    }

    private void displayQueueSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        if (event.getGuild().getSelfMember() == null || event.getGuild().getSelfMember().getVoiceState() == null || !event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("재생 목록 오류");
            builder.setDescription("봇이 보이스 채널에 연결되어 있지 않습니다.");
            event.replyEmbeds(builder.build()).queue();
            return;
        }
        PlayerManager.getINSTANCE().sendQueueEmbed(event.getChannel(), event.getGuild(), 1);
        event.reply("재생 목록을 표시합니다.").setEphemeral(true).queue();
    }
    private void displayNowPlayingSlash(SlashCommandInteractionEvent event) {
        PlayerManager playerManager = PlayerManager.getINSTANCE();
        EmbedBuilder embed = playerManager.createNowPlayingEmbed(event.getGuild());
        String buttonLabel = playerManager.isPaused(event.getGuild()) ? "▶️" : "⏸️";

        event.getChannel().sendMessageEmbeds(embed.build())
                .addActionRow(
                        Button.secondary("music:previous", "⏮️"),
                        Button.primary("music:togglepause", buttonLabel),
                        Button.secondary("music:skip", "⏭️"),
                        Button.secondary("music:showqueue", "📜 재생 목록")
                )
                .queue();
    }
}