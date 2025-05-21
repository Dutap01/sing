package org.github.Dutap01.bot.music;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

public class TrackScheduler extends AudioEventAdapter {
    public final AudioPlayer player;
    public final BlockingQueue<AudioTrack> queue;

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
    }

    public void queue(AudioTrack track) {
        if (!this.player.startTrack(track, true)) {
            this.queue.offer(track);
        }
    }

    public void nextTrack() {
        this.player.startTrack(this.queue.poll(), false);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            nextTrack();
        }
    }
    public boolean promoteTrack(int trackIndex) {
        if (trackIndex <= 0 || trackIndex > queue.size()) {
            return false;
        }
        List<AudioTrack> tempQueueList = new ArrayList<>();
        queue.drainTo(tempQueueList);

        if (tempQueueList.isEmpty()) {
            return false;
        }

        AudioTrack trackToPromote = tempQueueList.remove(trackIndex - 1);
        queue.add(trackToPromote);
        queue.addAll(tempQueueList);

        return true;
    }

    public BlockingQueue<AudioTrack> getQueue() {
        return queue;
    }

    public AudioPlayer getPlayer() {
        return player;
    }
}