package org.github.Dutap01.bot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import dev.lavalink.youtube.YoutubeAudioSourceManager;

import net.dv8tion.jda.api.EmbedBuilder;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

public class PlayerManager {
    private static PlayerManager INSTANCE;

    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager audioPlayerManager;

    public PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();

        YoutubeAudioSourceManager youtube = new YoutubeAudioSourceManager(true);
        this.audioPlayerManager.registerSourceManager(youtube);

        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
        AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager);
            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }
    public EmbedBuilder createNowPlayingEmbed(Guild guild) {
        final GuildMusicManager musicManager = getMusicManager(guild);
        final AudioTrack currentTrack = musicManager.getScheduler().getPlayer().getPlayingTrack();
        final BlockingQueue<AudioTrack> queue = musicManager.getScheduler().getQueue();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.BLUE);

        if (currentTrack == null) {
            embedBuilder.setTitle("🎧 현재 재생 중인 곡 없음")
                    .setDescription("`!재생 [URL/검색어]` 또는 `/play [URL/검색어]` 로 음악을 추가해주세요.");
            return embedBuilder;
        }

        embedBuilder.setTitle("🎧 현재 재생 중");
        embedBuilder.addField(
                "현재 재생 중:",
                "**" + currentTrack.getInfo().title + "** (" + formatDuration(currentTrack.getDuration()) + ")",
                false
        );
        AudioTrack nextTrack = queue.peek();
        if (nextTrack != null) {
            embedBuilder.addField(
                    "다음 곡:",
                    "**" + nextTrack.getInfo().title + "** (" + formatDuration(nextTrack.getDuration()) + ")",
                    false
            );
        } else {
            embedBuilder.addField("다음 곡:", "없음", false);
        };

        return embedBuilder;
    }
    public void sendQueueEmbed(MessageChannel channel, Guild guild, int page) {
        final GuildMusicManager musicManager = getMusicManager(guild);
        final BlockingQueue<AudioTrack> queue = musicManager.getScheduler().getQueue();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.ORANGE);

        List<AudioTrack> queueList = new ArrayList<>(queue);
        final int tracksPerPage = 10;
        int totalPages = (int) Math.ceil((double) queueList.size() / tracksPerPage);
        if (totalPages == 0 && !queueList.isEmpty()) totalPages = 1;
        if (totalPages == 0 && queueList.isEmpty()) totalPages = 1;

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        embedBuilder.setTitle(String.format("🎶 재생 목록 (페이지 %d/%d)", page, totalPages));


        List<Button> buttons = new ArrayList<>();
        List<Button> pageButtons = new ArrayList<>();

        if (queue.isEmpty()) {
            embedBuilder.setDescription("재생 목록이 비어 있습니다. `!play` 또는 `/play` 명령어로 음악을 추가하세요.");
        } else {
            StringBuilder sb = new StringBuilder();
            int startIndex = (page - 1) * tracksPerPage;
            int endIndex = Math.min(startIndex + tracksPerPage, queueList.size());
            for (int i = startIndex; i < endIndex; i++) {
                AudioTrack track = queueList.get(i);
                int trackNumber = i + 1;
                String emoji = getEmojiForNumber(trackNumber);

                sb.append(String.format("%s %s (%s)\n",
                        emoji,
                        track.getInfo().title,
                        formatDuration(track.getDuration())));
                buttons.add(Button.secondary("music:promote:" + trackNumber, emoji));
            }
            embedBuilder.setDescription(sb.toString());
        }
        if (page > 1) {
            pageButtons.add(Button.secondary("music:queue:prev_page:" + (page - 1), "⬅️ 이전 페이지"));
        }
        if (page < totalPages) {
            pageButtons.add(Button.secondary("music:queue:next_page:" + (page + 1), "다음 페이지 ➡️"));
        }

        MessageCreateAction action = channel.sendMessageEmbeds(embedBuilder.build());
        for (int i = 0; i < buttons.size(); i += 5) {
            List<Button> rowButtons = buttons.subList(i, Math.min(i + 5, buttons.size()));
            action = action.addActionRow(rowButtons);
        }
        if (!pageButtons.isEmpty()) {
            action = action.addActionRow(pageButtons);
        }

        action.queue();
    }

    private String formatDuration(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    private String getEmojiForNumber(int number) {
        if (number < 0) return "";
        List<String> numberEmojis = Arrays.asList(
                "0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣",
                "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"
        );

        if (number <= 9) {
            return numberEmojis.get(number);
        } else if (number == 10) {
            return "🔟";
        } else {
            StringBuilder sb = new StringBuilder();
            String numStr = String.valueOf(number);
            for (char digitChar : numStr.toCharArray()) {
                int digit = Character.getNumericValue(digitChar);
                if (digit >= 0 && digit <= 9) {
                    sb.append(numberEmojis.get(digit));
                } else {
                    sb.append(digitChar);
                }
            }
            return sb.toString();
        }
    }

    public void loadAndPlay(MessageChannel channel, String trackUrl, Member member) {
        final GuildMusicManager musicManager = this.getMusicManager(member.getGuild());

        this.audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.getScheduler().queue(track);
                EmbedBuilder embed = createNowPlayingEmbed(member.getGuild());
                channel.sendMessageEmbeds(embed.build())
                        .addActionRow(
                                Button.secondary("music:previous", "⏮️"),
                                Button.primary("music:togglepause", "⏯️"),
                                Button.secondary("music:skip", "⏭️"),
                                Button.secondary("music:showqueue", "📜 재생 목록")
                        )
                        .queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    AudioTrack firstTrack = playlist.getTracks().get(0);
                    musicManager.getScheduler().queue(firstTrack);
                    EmbedBuilder embed = createNowPlayingEmbed(member.getGuild());
                    channel.sendMessageEmbeds(embed.build())
                            .addActionRow(
                                    Button.secondary("music:previous", "⏮️"),
                                    Button.primary("music:togglepause", "⏯️"),
                                    Button.secondary("music:skip", "⏭️"),
                                    Button.secondary("music:showqueue", "📜 재생 목록")
                            )
                            .queue();
                } else {
                    for (AudioTrack track : playlist.getTracks()) {
                        musicManager.getScheduler().queue(track);
                    }
                    EmbedBuilder embed = createNowPlayingEmbed(member.getGuild());
                    channel.sendMessageEmbeds(embed.build())
                            .addActionRow(
                                    Button.secondary("music:previous", "⏮️"),
                                    Button.primary("music:togglepause", "⏯️"),
                                    Button.secondary("music:skip", "⏭️"),
                                    Button.secondary("music:showqueue", "📜 재생 목록")
                            )
                            .queue();
                }
            }

            @Override
            public void noMatches() {
                channel.sendMessage("⚠️ 요청하신 음악을 찾을 수 없습니다: " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("❌ 음악을 로드하지 못했습니다: " + exception.getMessage()).queue();
            }
        });
    }
    public void loadAndPlaySlash(InteractionHook hook, String trackUrl, Member member) {
        final GuildMusicManager musicManager = this.getMusicManager(member.getGuild());

        hook.setEphemeral(false).sendMessage("음악을 로드 중입니다...").queue(
                success -> {
                    this.audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
                        @Override
                        public void trackLoaded(AudioTrack track) {
                            musicManager.getScheduler().queue(track);
                            hook.editOriginal("음악을 큐에 추가했습니다.").queue();
                            hook.getInteraction().getMessageChannel().sendMessageEmbeds(createNowPlayingEmbed(member.getGuild()).build())
                                    .addActionRow(
                                            Button.secondary("music:previous", "⏮️"),
                                            Button.primary("music:togglepause", "⏯️"),
                                            Button.secondary("music:skip", "⏭️"),
                                            Button.secondary("music:showqueue", "📜 재생 목록")
                                    )
                                    .queue();
                        }

                        @Override
                        public void playlistLoaded(AudioPlaylist playlist) {
                            if (playlist.isSearchResult()) {
                                AudioTrack firstTrack = playlist.getTracks().get(0);
                                musicManager.getScheduler().queue(firstTrack);
                                hook.editOriginal("음악을 큐에 추가했습니다.").queue();
                                hook.getInteraction().getMessageChannel().sendMessageEmbeds(createNowPlayingEmbed(member.getGuild()).build())
                                        .addActionRow(
                                                Button.secondary("music:previous", "⏮️"),
                                                Button.primary("music:togglepause", "⏯️"),
                                                Button.secondary("music:skip", "⏭️"),
                                                Button.secondary("music:showqueue", "📜 재생 목록")
                                        )
                                        .queue();
                            } else {
                                for (AudioTrack track : playlist.getTracks()) {
                                    musicManager.getScheduler().queue(track);
                                }
                                hook.editOriginal("플레이리스트를 큐에 추가했습니다.").queue();
                                hook.getInteraction().getMessageChannel().sendMessageEmbeds(createNowPlayingEmbed(member.getGuild()).build())
                                        .addActionRow(
                                                Button.secondary("music:previous", "⏮️"),
                                                Button.primary("music:togglepause", "⏯️"),
                                                Button.secondary("music:skip", "⏭️"),
                                                Button.secondary("music:showqueue", "📜 재생 목록")
                                        )
                                        .queue();
                            }
                        }

                        @Override
                        public void noMatches() {
                            hook.editOriginal("⚠️ 요청하신 음악을 찾을 수 없습니다: " + trackUrl).queue();
                        }

                        @Override
                        public void loadFailed(FriendlyException exception) {
                            hook.editOriginal("❌ 음악을 로드하지 못했습니다: " + exception.getMessage()).queue();
                        }
                    });
                },
                failure -> {
                    System.err.println("initial response 메시지 전송 실패: " + failure.getMessage());
                }
        );
    }

    public void pauseTrack(Guild guild) {
        final GuildMusicManager musicManager = this.getMusicManager(guild);
        if (musicManager.getScheduler().getPlayer().getPlayingTrack() != null) {
            boolean isPaused = musicManager.getScheduler().getPlayer().isPaused();
            musicManager.getScheduler().getPlayer().setPaused(!isPaused);
        }
    }

    public void skipTrack(Guild guild) {
        final GuildMusicManager musicManager = this.getMusicManager(guild);
        musicManager.getScheduler().nextTrack();
    }
    public boolean promoteTrack(Guild guild, int trackIndex) {
        final GuildMusicManager musicManager = this.getMusicManager(guild);
        if (musicManager.getScheduler().getQueue().isEmpty()) {
            return false;
        }
        return musicManager.getScheduler().promoteTrack(trackIndex);
    }

    public AudioTrack getPlayingTrack(Guild guild) {
        return getMusicManager(guild).getScheduler().getPlayer().getPlayingTrack();
    }
    public BlockingQueue<AudioTrack> getQueue(Guild guild) {
        return this.getMusicManager(guild).getScheduler().getQueue();
    }
    public boolean isPaused(Guild guild) {
        return getMusicManager(guild).getScheduler().getPlayer().isPaused();
    }

    public static PlayerManager getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerManager();
        }
        return INSTANCE;
    }
}