package logicfl.utils;

import java.time.Instant;

import org.json.JSONObject;

import java.time.Duration;

public class Timer {

    private String name;
    private Instant start;
    private Instant end;

    public Timer(String name) {
        this.name = name;
        this.start = Instant.now();
        this.end = null;
    }

    public void setStart() {
        start = Instant.now();
    }

    public void setEnd() {
        end = Instant.now();
    }

    public String getName() {
        return this.name;
    }

    public Duration getElapsedTime() {
        return Duration.between(start, Instant.now());
    }

    public Duration getExecutionTime() {
        return Duration.between(start, end ==  null ? Instant.now() : end);
    }

    public String getExecTimeStr() {
        long execTime = getExecutionTime().toMillis();
        Duration duration = Duration.ofMillis(execTime);

        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        long milliseconds = execTime % 1000;

        String formattedTime = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
        return formattedTime;
    }

    public JSONObject getJsonObject() {
        JSONObject jsonObject = new JSONObject(); 
        jsonObject.put("exec_time_millis", getExecutionTime().toMillis());
        jsonObject.put("exec_time", getExecTimeStr());        
        
        return jsonObject;
    }
}
