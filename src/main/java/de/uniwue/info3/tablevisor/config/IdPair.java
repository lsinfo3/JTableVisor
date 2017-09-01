package de.uniwue.info3.tablevisor.config;

import org.projectfloodlight.openflow.types.TableId;

public class IdPair {
	public final int dataplaneId;
	public final int tableId;
	public final TableId tableIdObj;

	public IdPair(int dataplaneId, int tableId) {
		this.dataplaneId = dataplaneId;
		this.tableId = tableId;
		this.tableIdObj = TableId.of(tableId);
	}

	public IdPair(int dataplaneId, TableId tableId) {
		this.dataplaneId = dataplaneId;
		this.tableId = tableId.getValue();
		this.tableIdObj = tableId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		IdPair idPair = (IdPair) o;

		if (dataplaneId != idPair.dataplaneId) return false;
		return tableId == idPair.tableId;
	}

	@Override
	public int hashCode() {
		int result = dataplaneId;
		result = 31 * result + tableId;
		return result;
	}

	@Override
	public String toString() {
		return "(dpId="+dataplaneId+", tableId="+tableId+")";
	}
}
