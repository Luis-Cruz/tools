package pt.utl.ist.fenix.tools.excel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * @author Pedro Santos (pedro.miguel.santos@ist.utl.pt)
 * 
 * @param <Item>
 *            The type of object that is used to fill the lines.
 */
public abstract class SpreadsheetBuilder<Item> extends AbstractSpreadsheetBuilder<Item> {
    public abstract class ColumnGroup {
	private final ColumnBuilder[] columns;

	public ColumnGroup(ColumnBuilder... columns) {
	    this.columns = columns;
	}

	public abstract void fillHeader(HSSFWorkbook book, HSSFCell cell);

	@Override
	public String toString() {
	    StringBuilder builder = new StringBuilder();
	    builder.append(" [");
	    for (ColumnBuilder column : columns) {
		builder.append(column.toString());
		builder.append(", ");
	    }
	    builder.append("]");
	    return builder.toString();
	}
    }

    public class PropertyColumnGroup extends ColumnGroup {
	private String header = null;

	public PropertyColumnGroup(String headerKey, ResourceBundle headerBundle, ColumnBuilder... columns) {
	    super(columns);
	    if (headerKey != null && headerBundle != null) {
		this.header = headerBundle.getString(headerKey);
	    }
	}

	@Override
	public void fillHeader(HSSFWorkbook book, HSSFCell cell) {
	    setHeaderValue(book, cell, header);
	}

	@Override
	public String toString() {
	    return header + super.toString();
	}
    }

    public class FormatColumnGroup extends ColumnGroup {
	private final String header;

	public FormatColumnGroup(String formatKey, ResourceBundle headerBundle, String[] args, ColumnBuilder... columns) {
	    super(columns);
	    String format = headerBundle.getString(formatKey);
	    Pattern pattern = Pattern.compile("\\{(\\d)\\}");
	    Matcher matcher = pattern.matcher(format);
	    while (matcher.find()) {
		int index = Integer.parseInt(matcher.group(1));
		format = format.replace(matcher.group(), args[index]);
	    }
	    this.header = format;
	}

	@Override
	public void fillHeader(HSSFWorkbook book, HSSFCell cell) {
	    setHeaderValue(book, cell, header);
	}

	@Override
	public String toString() {
	    return header + super.toString();
	}
    }

    public abstract class ColumnBuilder {
	private String header = null;

	public ColumnBuilder(String headerKey, ResourceBundle headerBundle) {
	    if (headerKey != null && headerBundle != null) {
		this.header = headerBundle.getString(headerKey);
	    }
	}

	public void fillHeader(HSSFWorkbook book, HSSFCell cell) {
	    setHeaderValue(book, cell, header);
	}

	/**
	 * Extend and call {@link #setValue(HSSFWorkbook, HSSFCell, Object)}
	 */
	public abstract void fillCell(HSSFWorkbook book, HSSFCell cell, Item item);

	@Override
	public String toString() {
	    return "(" + header + ")";
	}
    }

    public class FormatColumnBuilder extends ColumnBuilder {
	private final String format;

	public FormatColumnBuilder(String headerKey, ResourceBundle headerBundle, String format) {
	    super(headerKey, headerBundle);
	    this.format = format;
	}

	@Override
	public void fillCell(HSSFWorkbook book, HSSFCell cell, Item item) {
	    setValue(book, cell, getFormattedProperties(format, item));
	}
    }

    public class PropertyColumnBuilder extends ColumnBuilder {
	private final String property;

	public PropertyColumnBuilder(String headerKey, ResourceBundle headerBundle, String property) {
	    super(headerKey, headerBundle);
	    this.property = property;
	}

	@Override
	public void fillCell(HSSFWorkbook book, HSSFCell cell, Item item) {
	    try {
		setValue(book, cell, PropertyUtils.getProperty(item, property));
	    } catch (Exception e) {
		throw new RuntimeException("could not read property '" + property + "' from object " + item, e);
	    }
	}

	@Override
	public String toString() {
	    return super.toString() + ":" + property;
	}
    }

    public class NullSafePropertyColumnBuilder extends PropertyColumnBuilder {
	private final String nullCheck;

	public NullSafePropertyColumnBuilder(String headerKey, ResourceBundle headerBundle, String property, String nullCheck) {
	    super(headerKey, headerBundle, property);
	    this.nullCheck = nullCheck;
	}

	@Override
	public void fillCell(HSSFWorkbook book, HSSFCell cell, Item item) {
	    try {
		if (PropertyUtils.getProperty(item, nullCheck) != null)
		    super.fillCell(book, cell, item);
	    } catch (Exception e) {
		throw new RuntimeException("could not read property '" + nullCheck + "' from object " + item, e);
	    }
	}
    }

    private final Collection<Item> items;

    private boolean hasHeader = true;

    private int startRow = 0;

    private int startColumn = 0;

    protected abstract List<ColumnBuilder> getColumns();

    protected abstract List<ColumnGroup> getColumnGroups();

    public SpreadsheetBuilder(Collection<Item> items) {
	this.items = items;
    }

    public SpreadsheetBuilder<Item> hideHeader() {
	hasHeader = false;
	return this;
    }

    public SpreadsheetBuilder<Item> setStartRow(int start) {
	startRow = start;
	return this;
    }

    public SpreadsheetBuilder<Item> setStartColumn(int start) {
	startColumn = start;
	return this;
    }

    public static String getFormattedProperties(String format, Object object) {
	// "${a.b} - ${a.c} - ${b,-4.5tY}"
	// String.format("%s - %s - %-4.5tY", object.getA().getB(),
	// object.getA().getC(), object.getB())

	// TODO: use a separator different than ',' because the comma can be
	// used as a flag in the format

	List<Object> args = new ArrayList<Object>();
	StringBuilder builder = new StringBuilder();

	if (format != null) {
	    int lastIndex = 0, index;

	    while ((index = format.indexOf("${", lastIndex)) != -1) {
		int end = format.indexOf("}", index + 2);

		if (end == -1) {
		    throw new RuntimeException("'" + format + "':unmatched group at pos " + index);
		}

		builder.append(format.substring(lastIndex, index));
		lastIndex = end + 1;

		if (end - index == 2) {
		    builder.append("%s");
		    args.add(object);
		} else {
		    String spec = format.substring(index + 2, end);
		    String[] parts = spec.split(",");

		    String property = parts[0];

		    if (parts.length > 1) {
			builder.append("%" + parts[1]);
		    } else {
			builder.append("%s");
		    }

		    try {
			Object value = PropertyUtils.getProperty(object, property);

			args.add(value);

		    } catch (Exception e) {
			throw new RuntimeException("could not retrieve property '" + property + "' for object " + object, e);
		    }
		}
	    }

	    builder.append(format.substring(lastIndex));
	}

	return String.format(builder.toString(), args.toArray());
    }

    @Override
    final void build(WorkbookBuilder bookBuilder) {
	List<ColumnBuilder> columns = getColumns();
	Map<ColumnBuilder, ColumnGroup> groupMap = new HashMap<ColumnBuilder, ColumnGroup>();
	for (ColumnGroup group : getColumnGroups()) {
	    for (ColumnBuilder column : group.columns) {
		groupMap.put(column, group);
	    }
	}
	HSSFWorkbook book = bookBuilder.getExcelBook();
	HSSFSheet sheet = book.createSheet();
	int rownum = startRow;
	int colnum = startColumn;
	if (hasHeader) {
	    if (!groupMap.isEmpty()) {
		List<ColumnGroup> used = new ArrayList<ColumnGroup>();
		HSSFRow header = sheet.createRow(rownum++);
		for (ColumnBuilder column : columns) {
		    if (groupMap.containsKey(column)) {
			ColumnGroup group = groupMap.get(column);
			if (!used.contains(group)) {
			    HSSFCell cell = header.createCell(colnum);
			    group.fillHeader(book, cell);
			    CellRangeAddress range = new CellRangeAddress(cell.getRowIndex(), cell.getRowIndex(), cell
				    .getColumnIndex(), cell.getColumnIndex() + group.columns.length - 1);
			    cell.getRow().getSheet().addMergedRegion(range);
			    colnum = header.getLastCellNum() + group.columns.length - 1;
			    used.add(group);
			}
		    } else {
			HSSFCell cell = header.createCell(colnum);
			column.fillHeader(book, cell);
			CellRangeAddress range = new CellRangeAddress(cell.getRowIndex(), cell.getRowIndex() + 1, cell
				.getColumnIndex(), cell.getColumnIndex());
			cell.getRow().getSheet().addMergedRegion(range);
			colnum = header.getLastCellNum();
		    }
		}
		colnum = startColumn;
	    }
	    HSSFRow header = sheet.createRow(rownum++);
	    for (ColumnBuilder column : columns) {
		column.fillHeader(book, header.createCell(colnum));
		colnum = header.getLastCellNum();
	    }
	}
	for (Item item : items) {
	    HSSFRow row = sheet.createRow(rownum++);
	    colnum = startColumn;
	    for (ColumnBuilder column : columns) {
		column.fillCell(book, row.createCell(colnum), item);
		colnum = row.getLastCellNum();
	    }
	}
	for (int i = 0; i < sheet.getLastRowNum(); i++) {
	    sheet.autoSizeColumn(i);
	}
    }
}
