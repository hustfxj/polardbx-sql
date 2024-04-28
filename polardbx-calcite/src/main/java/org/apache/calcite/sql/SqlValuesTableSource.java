package org.apache.calcite.sql;

import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author pangzhaoxing
 *
 * for values statement
 */
public class SqlValuesTableSource extends SqlCall {

    public static final SqlValuesTableSourceOperator OPERATOR = new SqlValuesTableSourceOperator();

    public static final String VALUES_TABLE_NAME = "values_table";

    public static final String COLUMN_NAME_PREFIX = "column_";

    List<SqlNode> operands;

    public SqlValuesTableSource(SqlParserPos pos, List<SqlNode> operands){
        super(pos);
        this.operands = operands;
    }

    @Override
    public SqlKind getKind() {
        return SqlKind.VALUES;
    }

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
        return operands;
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        if (!writer.inQuery()) {
            final SqlWriter.Frame frame =
                writer.startList(SqlWriter.FrameTypeEnum.SUB_QUERY, "(", ")");
            getOperator().unparse(writer, this, 0, 0);
            writer.endList(frame);
        } else {
            getOperator().unparse(writer, this, leftPrec, rightPrec);
        }
    }


    public static class SqlValuesTableSourceOperator extends SqlSpecialOperator{

        public SqlValuesTableSourceOperator() {
            super("VALUES_TABLE_SOURCE", SqlKind.VALUES);
        }

        @Override
        public void unparse(
            SqlWriter writer,
            SqlCall call,
            int leftPrec,
            int rightPrec) {
            final SqlWriter.Frame frame =
                writer.startList(SqlWriter.FrameTypeEnum.VALUES, "VALUES ", "");
            for (SqlNode operand : call.getOperandList()) {
                writer.sep(",");
                SqlWriter.Frame rowFrame = writer.startList(SqlWriter.FrameTypeEnum.FUN_CALL, "ROW", "");
                operand.unparse(writer, 0, 0);
                writer.endList(rowFrame);
            }
            writer.endList(frame);
        }

        @Override
        public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
            return new SqlValuesTableSource(pos, Arrays.asList(operands));
        }

    }


}