package com.cwctravel.hudson.plugins.script_scm;

import hudson.model.User;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.EditType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScriptSCMChangeLogEntry extends Entry {
	public static class Item implements AffectedFile {
		private String filename;
		private String changeType;

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}

		public String getChangeType() {
			return changeType;
		}

		public void setChangeType(String changeType) {
			this.changeType = changeType;
		}

		public String getPath() {
			return filename;
		}

		public EditType getEditType() {
			if("add".equals(changeType)) {
				return EditType.ADD;
			}
			else if("edit".equals(changeType)) {
				return EditType.EDIT;
			}
			else if("delete".equals(changeType)) {
				return EditType.DELETE;
			}
			return null;
		}
	}

	private String changesetNumber;
	private String date;
	private String userId;
	private String comment;
	private List<Item> files;

	public String getChangesetNumber() {
		return changesetNumber;
	}

	public void setChangesetNumber(String changesetNumber) {
		this.changesetNumber = changesetNumber;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public List<Item> getFiles() {
		return files;
	}

	public void setFiles(List<Item> files) {
		this.files = files;
	}

	public void addFile(Item item) {
		if(files == null) {
			files = new ArrayList<Item>();
		}
		files.add(item);
	}

	@Override
	public String getMsg() {
		return comment;
	}

	@Override
	public User getAuthor() {
		return User.get(userId);
	}

	@Override
	public Collection<String> getAffectedPaths() {
		List<String> result = new ArrayList<String>();
		if(files != null) {
			for(Item item: files) {
				result.add(item.getFilename());
			}
		}

		return result;
	}

	@Override
	public Collection<? extends AffectedFile> getAffectedFiles() {
		return getFiles();
	}

}
