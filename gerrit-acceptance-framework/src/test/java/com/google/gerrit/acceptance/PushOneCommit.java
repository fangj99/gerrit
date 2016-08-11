import com.google.common.collect.ImmutableMap;
import java.util.Map;
    PushOneCommit create(
        ReviewDb db,
        PersonIdent i,
        TestRepository<?> testRepo,
        @Assisted String subject,
        @Assisted Map<String, String> files);

  private final Map<String, String> files;
  @AssistedInject
  PushOneCommit(ChangeNotes.Factory notesFactory,
      ApprovalsUtil approvalsUtil,
      Provider<InternalChangeQuery> queryProvider,
      @Assisted ReviewDb db,
      @Assisted PersonIdent i,
      @Assisted TestRepository<?> testRepo,
      @Assisted String subject,
      @Assisted Map<String, String> files) throws Exception {
    this(notesFactory, approvalsUtil, queryProvider, db, i, testRepo,
        subject, files, null);
  }

    this(notesFactory, approvalsUtil, queryProvider, db, i, testRepo,
        subject, ImmutableMap.of(fileName, content), changeId);
  }

  private PushOneCommit(ChangeNotes.Factory notesFactory,
      ApprovalsUtil approvalsUtil,
      Provider<InternalChangeQuery> queryProvider,
      ReviewDb db,
      PersonIdent i,
      TestRepository<?> testRepo,
      String subject,
      Map<String, String> files,
      String changeId) throws Exception {
    this.files = files;
  public void setParent(RevCommit parent) throws Exception {
    commitBuilder.noParents();
    commitBuilder.parent(parent);
  }

    for (Map.Entry<String, String> e : files.entrySet()) {
      commitBuilder.add(e.getKey(), e.getValue());
    }
    for (String fileName : files.keySet()) {
      commitBuilder.rm(fileName);
    }