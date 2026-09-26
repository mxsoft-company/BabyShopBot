package uz.mxsell.workers_salary_platform.bot.state;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserStateManager {

    private final Map<ChatKey, BotState> states = new ConcurrentHashMap<>();
    private final Map<ChatKey, AppealDraft> appealDrafts = new ConcurrentHashMap<>();

    public BotState getState(ChatKey key) {
        return states.getOrDefault(key, BotState.MAIN_MENU);
    }

    public void setState(ChatKey key, BotState state) {
        states.put(key, state);
    }

    public void setPendingAppealType(ChatKey key, String type) {
        appealDrafts.merge(key, new AppealDraft(type, null),
                (oldDraft, newDraft) -> new AppealDraft(newDraft.type(), oldDraft.text()));
    }

    public String getPendingAppealType(ChatKey key) {
        AppealDraft draft = appealDrafts.get(key);
        return draft != null ? draft.type() : null;
    }

    public void setPendingAppealText(ChatKey key, String text) {
        appealDrafts.merge(key, new AppealDraft(null, text),
                (oldDraft, newDraft) -> new AppealDraft(oldDraft.type(), newDraft.text()));
    }

    public String getPendingAppealText(ChatKey key) {
        AppealDraft draft = appealDrafts.get(key);
        return draft != null ? draft.text() : null;
    }

    public void clearPendingAppeal(ChatKey key) {
        appealDrafts.remove(key);
    }

    public record AppealDraft(String type, String text) {
    }
}
