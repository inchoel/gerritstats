package com.holmsted.gerrit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import jsonutils.JsonUtils;


public class Commit {
    private static final long SEC_TO_MSEC = 1000;

    public enum PatchSetKind {
        REWORK,
        TRIVIAL_REBASE,
        NO_CODE_CHANGE,
        NO_CHANGE
    }

    public static class Identity {
        public String name;
        public String email;
        public String username;

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getUsername() {
            return username;
        }

        public static Identity fromJson(JSONObject ownerJson) {
            Identity identity = new Identity();
            identity.name = ownerJson.optString("name");
            identity.email = ownerJson.optString("email");
            identity.username = ownerJson.optString("username");
            return identity;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Identity)) {
                return false;
            }
            Identity otherIdentity = (Identity) other;
            if (email != null) {
                return email.equals(otherIdentity.email);
            } else {
                return super.equals(other);
            }
        }

        public int compareTo(@Nonnull Identity other) {
            if (email != null && other.email != null) {
                return email.compareTo(other.email);
            } else if (username != null && other.username != null) {
                return username.compareTo(other.username);
            } else {
                return 0;
            }
        }

        @Override
        public int hashCode() {
            if (email != null) {
                return email.hashCode();
            } else {
                return super.hashCode();
            }
        }

        @Override
        public String toString() {
            if (email != null && !email.isEmpty()) {
                return email;
            } else if (name != null && !name.isEmpty()) {
                return name;
            } else if (username != null && !username.isEmpty()) {
                return username;
            } else {
                return super.toString();
            }
        }
    }

    public static class ChangeComment {
        public long timestamp;
        public Identity reviewer;
        public String message;

        static ChangeComment fromJson(JSONObject commentJson) {
            ChangeComment comment = new ChangeComment();
            comment.timestamp = commentJson.optLong("timestamp") * SEC_TO_MSEC;
            comment.reviewer = Identity.fromJson(commentJson.optJSONObject("reviewer"));
            comment.message = commentJson.optString("message");
            return comment;
        }
    }

    public static class Approval {
        public String type;
        public String description;
        public int value;
        public long grantedOnDate;
        public Identity grantedBy;

        public static List<Approval> fromJson(JSONArray approvals) {
            List<Approval> result = new ArrayList<Approval>();
            if (approvals != null) {
                for (int i = 0; i < approvals.length(); ++i) {
                    result.add(Approval.fromJson(approvals.getJSONObject(i)));
                }
            }
            return result;
        }

        public static Approval fromJson(JSONObject approvalJson) {
            Approval approval = new Approval();
            approval.type = approvalJson.optString("type");
            approval.description = approvalJson.optString("description");
            approval.value = approvalJson.optInt("value");
            approval.grantedOnDate = approvalJson.optLong("grantedOn") * SEC_TO_MSEC;
            approval.grantedBy = Identity.fromJson(approvalJson.getJSONObject("by"));
            return approval;
        }
    }

    public static class PatchSetComment {
        public String file;
        public int line;
        public Identity reviewer;
        public String message;

        public String getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }

        public Identity getReviewer() {
            return reviewer;
        }

        public String getMessage() {
            return message;
        }

        public static List<PatchSetComment> fromJson(JSONArray comments) {
            List<PatchSetComment> result = new ArrayList<PatchSetComment>();
            if (comments != null) {
                for (int i = 0; i < comments.length(); ++i) {
                    result.add(PatchSetComment.fromJson(comments.getJSONObject(i)));
                }
            }
            return result;
        }

        public static PatchSetComment fromJson(JSONObject commentJson) {
            PatchSetComment comment = new PatchSetComment();
            comment.file = commentJson.optString("file");
            comment.line = commentJson.optInt("line");
            comment.reviewer = Identity.fromJson(commentJson.getJSONObject("reviewer"));
            comment.message = commentJson.optString("message");
            return comment;
        }
    }

    public static class PatchSet {
        public int number;
        public String revision;
        public final List<String> parents = new ArrayList<String>();
        public String ref;
        public Identity uploader;
        public long createdOnDate;
        public Identity author;
        public boolean isDraft;
        public PatchSetKind kind;
        public final List<Approval> approvals = new ArrayList<Approval>();
        public final List<PatchSetComment> comments = new ArrayList<PatchSetComment>();
        public int sizeInsertions;
        public int sizeDeletions;

        static PatchSet fromJson(JSONObject patchSetJson) {
            PatchSet patchSet = new PatchSet();
            patchSet.number = patchSetJson.optInt("number");
            patchSet.revision = patchSetJson.optString("revision");
            patchSet.parents.addAll(JsonUtils.readStringArray(patchSetJson.optJSONArray("parents")));
            patchSet.ref = patchSetJson.optString("ref");
            patchSet.uploader = Identity.fromJson(patchSetJson.optJSONObject("uploader"));
            patchSet.createdOnDate = patchSetJson.optLong("createdOn") * SEC_TO_MSEC;
            patchSet.author = Identity.fromJson(patchSetJson.optJSONObject("author"));
            patchSet.isDraft = patchSetJson.optBoolean("isDraft");

            String patchSetKind = patchSetJson.optString("kind");
            try {
                patchSet.kind = PatchSetKind.valueOf(patchSetKind);
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown patch set kind " + patchSetKind);
            }

            patchSet.approvals.addAll(Approval.fromJson(patchSetJson.optJSONArray("approvals")));
            patchSet.comments.addAll(PatchSetComment.fromJson(patchSetJson.optJSONArray("comments")));
            patchSet.sizeInsertions = patchSetJson.optInt("sizeInsertions");
            patchSet.sizeDeletions = patchSetJson.optInt("sizeDeletions");
            return patchSet;
        }
    }

    public String project;
    public String branch;
    public String id;
    public int commitNumber;
    public String subject;
    public Identity owner;
    public String url;
    public String commitMessage;
    public long createdOnDate;
    public long lastUpdatedDate;
    public boolean isOpen;
    public String status;

    public final List<Identity> reviewers = new ArrayList<Identity>();
    public final List<ChangeComment> comments = new ArrayList<ChangeComment>();
    public final List<PatchSet> patchSets = new ArrayList<PatchSet>();

    public int getPatchSetCountForKind(PatchSetKind kind) {
        int count = 0;
        for (PatchSet patchSet : patchSets) {
            if (patchSet.kind == kind) {
                ++count;
            }
        }
        return count;
    }

    public String getUrl() {
        return url;
    }

    public List<PatchSet> getPatchSets() {
        return patchSets;
    }

    static boolean isCommit(JSONObject lineJson) {
        return lineJson.opt("status") != null;
    }

    static Commit fromJson(JSONObject commitJson) {
        Commit commit = new Commit();
        commit.project = commitJson.optString("project");
        commit.branch = commitJson.optString("branch");

        commit.id = commitJson.optString("id");
        commit.commitNumber = commitJson.optInt("number");
        commit.subject = commitJson.optString("subject");
        commit.owner = Identity.fromJson(commitJson.optJSONObject("owner"));
        commit.url = commitJson.optString("url");
        commit.commitMessage = commitJson.optString("commitMessage");
        commit.createdOnDate = commitJson.optLong("createdOn") * SEC_TO_MSEC;
        commit.lastUpdatedDate = commitJson.optLong("lastUpdated") * SEC_TO_MSEC;
        commit.isOpen = commitJson.optBoolean("open");
        commit.status = commitJson.optString("status");

        commit.setReviewersFromJson(commitJson.optJSONArray("allReviewers"));
        commit.setCommentsFromJson(commitJson.optJSONArray("comments"));
        commit.setPatchSetsFromJson(commitJson.optJSONArray("patchSets"));

        return commit;
    }

    private void setReviewersFromJson(JSONArray reviewers) {
        if (reviewers != null) {
            for (int i = 0; i < reviewers.length(); ++i) {
                JSONObject identityJson = reviewers.getJSONObject(i);
                this.reviewers.add(Identity.fromJson(identityJson));
            }
        }
    }

    private void setCommentsFromJson(JSONArray comments) {
        for (int i = 0; i < comments.length(); ++i) {
            JSONObject commentJson = comments.getJSONObject(i);
            this.comments.add(ChangeComment.fromJson(commentJson));
        }
    }

    private void setPatchSetsFromJson(JSONArray patchSets) {
        for (int i = 0; i < patchSets.length(); ++i) {
            JSONObject patchSetJson = patchSets.getJSONObject(i);
            this.patchSets.add(PatchSet.fromJson(patchSetJson));
        }
    }
}