package pt.ist.dbUtils;

import java.sql.SQLException;

public abstract class TaskWithExternalDbOperation extends TaskWithExternalDbOperation_Base {

    private static ThreadLocal<DbTransaction> transaction = new InheritableThreadLocal<DbTransaction>();

    private class EmbededExternalDbOperation extends ExternalDbOperation {

	private final TaskWithExternalDbOperation instance;

	public EmbededExternalDbOperation(final TaskWithExternalDbOperation instance) {
	    this.instance = instance;
	}

	@Override
	protected void doOperation() throws SQLException {
	    instance.doOperation();
	}

	@Override
	protected String getDbPropertyPrefix() {
	    return instance.getDbPropertyPrefix();
	}

    }

    @Override
    public final void executeTask() {
	try {
	    final EmbededExternalDbOperation embededExternalDbOperation = new EmbededExternalDbOperation(this);
	    transaction.set(embededExternalDbOperation);
	    embededExternalDbOperation.execute();
	} finally {
	    transaction.remove();
	}
    }

    protected void executeQuery(final ExternalDbQuery externalDbQuery) throws SQLException {
	final DbTransaction dbTransaction = transaction.get();
	if (dbTransaction == null) {
	    throw new Error("error.not.inside.transaction");
	}
	dbTransaction.executeQuery(externalDbQuery);
    }

    protected abstract String getDbPropertyPrefix();

    protected abstract void doOperation() throws SQLException;

}
