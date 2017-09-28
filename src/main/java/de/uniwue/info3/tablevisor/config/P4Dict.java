package de.uniwue.info3.tablevisor.config;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class P4Dict {
	private static final Pattern pTable1 = Pattern.compile("// *@TV *table *(\\d+)");
	private static final Pattern pTable2 = Pattern.compile("table *([^ ]+) *\\{");
	private static final Pattern pField1 = Pattern.compile("// *@TV *field *([^ ]+)");
	private static final Pattern pField2 = Pattern.compile("([^ ]+) *: *[^ ]+ *;");
	private static final Pattern pAction1 = Pattern.compile("// *@TV *action *([^ ]+)((?: *[^ =]+=[^ =]+)*)");
	private static final Pattern pAction2 = Pattern.compile("action *([^ ()]+) *\\(([^()]*)\\) *\\{");
	private static final Pattern pMisc = Pattern.compile("// *@TV .*");

	private HashMap<String, Integer> p4TablesToIds = new HashMap<>();
	private HashMap<Integer, String> tableIdsToP4Names = new HashMap<>();
	private HashMap<String, String> p4FieldToOfField = new HashMap<>();
	private HashMap<String, String> ofFieldToP4Field = new HashMap<>();
	private HashMap<String, HashSet<String>> p4ActionToOfAction = new HashMap<>();
	private HashMap<HashSet<String>, String> ofActionToP4Action = new HashMap<>();
	private HashMap<String, String> p4ParamToOfParam = new HashMap<>();
	private HashMap<String, String> ofParamToP4Param = new HashMap<>();

	public void parseP4File(String path) {
		try {
			LineNumberReader lnr = new LineNumberReader(new FileReader(path));

			String line;
			int lineNr = 0;
			while ((line = lnr.readLine()) != null) {
				line = line.trim();
				lineNr++;

				Matcher mTable1 = pTable1.matcher(line);
				Matcher mField1 = pField1.matcher(line);
				Matcher mAction1 = pAction1.matcher(line);
				Matcher mMisc = pMisc.matcher(line);

				if (mTable1.matches()) {
					line = lnr.readLine().trim(); lineNr++;
					Matcher mTable2 = pTable2.matcher(line);
					if (!mTable2.matches()) {
						throw new IllegalArgumentException("table definition expected in line " + lineNr);
					}
					p4TablesToIds.put(mTable2.group(1).toLowerCase(), Integer.parseInt(mTable1.group(1)));
					tableIdsToP4Names.put(Integer.parseInt(mTable1.group(1)), mTable2.group(1));
				}

				else if (mField1.matches()) {
					line = lnr.readLine().trim(); lineNr++;
					Matcher mField2 = pField2.matcher(line);
					if (!mField2.matches()) {
						throw new IllegalArgumentException("field definition expected in line " + lineNr);
					}
					p4FieldToOfField.put(mField2.group(1).toLowerCase(), mField1.group(1));
					ofFieldToP4Field.put(mField1.group(1).toLowerCase(), mField2.group(1));
				}

				else if (mAction1.matches()) {
					HashSet<String> commands = new HashSet<>();
					do {
						commands.add(mAction1.group(1).toLowerCase());

						String params = mAction1.group(2);
						if (params != null && !params.isEmpty()) {
							for (String param : params.split(" ")) {
								param = param.trim();
								if (param.isEmpty()) continue;

								String[] p = param.split("=");
								p4ParamToOfParam.put(p[1].toLowerCase(), p[0]);
								ofParamToP4Param.put(p[0].toLowerCase(), p[1]);
							}
						}

						line = lnr.readLine().trim(); lineNr++;
						mAction1 = pAction1.matcher(line);
					} while (mAction1.matches());

					Matcher mAction2 = pAction2.matcher(line);
					if (!mAction2.matches()) {
						throw new IllegalArgumentException("action definition expected in line " + lineNr);
					}

					p4ActionToOfAction.put(mAction2.group(1).toLowerCase(), commands);
					ofActionToP4Action.put(commands, mAction2.group(1));
				}

				else if (mMisc.matches()) {
					throw new IllegalArgumentException("Unrecognized TV annotation format in line " + lineNr);
				}
			}
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public Integer p4TableToId(String p4TableName) {
		return p4TablesToIds.get(p4TableName.toLowerCase());
	}

	public String tableIdToP4Name(Integer tableId) {
		return tableIdsToP4Names.get(tableId);
	}

	public String p4FieldToOfField(String p4Field) {
		return p4FieldToOfField.get(p4Field.toLowerCase());
	}

	public String ofFieldToP4Field(String ofField) {
		return ofFieldToP4Field.get(ofField.toLowerCase());
	}

	public HashSet<String> p4ActionToOfAction(String p4Action) {
		return new HashSet<>(p4ActionToOfAction.get(p4Action.toLowerCase()));
	}

	public String ofActionToP4Action(HashSet<String> ofAction) {
		HashSet<String> copy = new HashSet<>();
		for (String s : ofAction) copy.add(s.toLowerCase());
		return ofActionToP4Action.get(copy);
	}

	public String p4ParamToOfParam(String p4Param) {
		return p4ParamToOfParam.get(p4Param.toLowerCase());
	}

	public String ofParamToP4Param(String ofParam) {
		return ofParamToP4Param.get(ofParam.toLowerCase());
	}
}
