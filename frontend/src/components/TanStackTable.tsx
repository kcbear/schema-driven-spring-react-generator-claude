/**
 * Purpose: generic, schema-agnostic table used by every generated list view. It reads the
 * current page of records from react-admin's list context and renders them with TanStack
 * Table (headless) + MUI markup. Paging and sorting are SERVER-SIDE (manualPagination /
 * manualSorting): the table only holds one page, so 12,000+ rows stay fast. A row click opens
 * the react-admin edit form. data-testid attributes are used by the Playwright tests.
 */
import { useMemo } from 'react';
import { useCreatePath, useListContext } from 'react-admin';
import { useNavigate } from 'react-router-dom';
import { flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table';
import type { ColumnDef, PaginationState } from '@tanstack/react-table';
import {
  Box,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
} from '@mui/material';

export type ColumnKind = 'text' | 'number' | 'boolean' | 'date' | 'datetime' | 'enum';

/** Column description emitted by the generator from a schema field. */
export interface ColumnSpec {
  key: string;
  label: string;
  kind: ColumnKind;
  sortable: boolean;
}

interface Props {
  columns: ColumnSpec[];
  resource: string;
}

const EMPTY: any[] = [];
const PAGE_SIZES = [10, 25, 50, 100];

/** Formats a raw JSON value for display according to the column kind. */
function formatCell(value: unknown, kind: ColumnKind): string {
  if (value === null || value === undefined || value === '') return '';
  switch (kind) {
    case 'number':
      return Number(value).toLocaleString('en-US', { maximumFractionDigits: 4 });
    case 'boolean':
      return value ? '\u2713' : '\u2717';
    case 'datetime':
      return new Date(String(value)).toISOString().replace('T', ' ').slice(0, 19);
    default:
      return String(value);
  }
}

export const TanStackTable = ({ columns, resource }: Props) => {
  const { data, total, isLoading, page, perPage, setPage, setPerPage, sort, setSort } = useListContext();
  const navigate = useNavigate();
  const createPath = useCreatePath();

  const defs = useMemo<ColumnDef<any>[]>(
    () =>
      columns.map((c) => ({
        accessorKey: c.key,
        header: c.label,
        enableSorting: c.sortable,
        cell: (ctx) => formatCell(ctx.getValue(), c.kind),
      })),
    [columns],
  );

  const totalRows = total ?? 0;
  const table = useReactTable({
    data: data ?? EMPTY,
    columns: defs,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    manualSorting: true,
    pageCount: Math.max(1, Math.ceil(totalRows / perPage)),
    state: { pagination: { pageIndex: page - 1, pageSize: perPage } },
    onPaginationChange: (updater) => {
      const current: PaginationState = { pageIndex: page - 1, pageSize: perPage };
      const next = typeof updater === 'function' ? updater(current) : updater;
      if (next.pageSize !== perPage) {
        setPerPage(next.pageSize);
        setPage(1);
      } else {
        setPage(next.pageIndex + 1);
      }
    },
  });

  const from = totalRows === 0 ? 0 : (page - 1) * perPage + 1;
  const to = Math.min(page * perPage, totalRows);

  return (
    <Box>
      <TableContainer>
        <Table size="small" data-testid="ts-table">
          <TableHead>
            {table.getHeaderGroups().map((hg) => (
              <TableRow key={hg.id}>
                {hg.headers.map((h) => {
                  const field = h.column.id;
                  const active = sort?.field === field;
                  return (
                    <TableCell key={h.id} sortDirection={active ? (sort.order === 'DESC' ? 'desc' : 'asc') : false}>
                      {h.column.getCanSort() ? (
                        <TableSortLabel
                          active={active}
                          direction={active && sort.order === 'DESC' ? 'desc' : 'asc'}
                          onClick={() =>
                            setSort({ field, order: active && sort.order === 'ASC' ? 'DESC' : 'ASC' })
                          }
                        >
                          {flexRender(h.column.columnDef.header, h.getContext())}
                        </TableSortLabel>
                      ) : (
                        flexRender(h.column.columnDef.header, h.getContext())
                      )}
                    </TableCell>
                  );
                })}
              </TableRow>
            ))}
          </TableHead>
          <TableBody>
            {table.getRowModel().rows.map((row) => (
              <TableRow
                key={row.id}
                hover
                data-testid="row"
                sx={{ cursor: 'pointer' }}
                onClick={() => navigate(createPath({ resource, type: 'edit', id: row.original.id }))}
              >
                {row.getVisibleCells().map((cell) => (
                  <TableCell key={cell.id} data-testid={'cell-' + cell.column.id}>
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, p: 1, justifyContent: 'flex-end' }}>
        {isLoading && <span>Loading...</span>}
        <label>
          Rows per page{' '}
          <select
            data-testid="page-size"
            value={perPage}
            onChange={(e) => table.setPageSize(Number(e.target.value))}
          >
            {PAGE_SIZES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        <span data-testid="page-info">
          {from}-{to} of {totalRows}
        </span>
        <Button size="small" aria-label="First page" disabled={!table.getCanPreviousPage()} onClick={() => table.setPageIndex(0)}>
          {'\u00AB'}
        </Button>
        <Button size="small" aria-label="Previous page" disabled={!table.getCanPreviousPage()} onClick={() => table.previousPage()}>
          {'\u2039'}
        </Button>
        <Button size="small" aria-label="Next page" disabled={!table.getCanNextPage()} onClick={() => table.nextPage()}>
          {'\u203A'}
        </Button>
        <Button size="small" aria-label="Last page" disabled={!table.getCanNextPage()} onClick={() => table.setPageIndex(table.getPageCount() - 1)}>
          {'\u00BB'}
        </Button>
      </Box>
    </Box>
  );
};
