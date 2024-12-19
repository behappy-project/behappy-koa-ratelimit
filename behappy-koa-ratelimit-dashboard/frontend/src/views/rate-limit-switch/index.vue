<template>
  <div class="app-container">
    <div style="margin: 10px 0">
      <el-select v-model="env" filterable placeholder="请选择环境">
        <el-option label="release" value="release"/>
        <el-option label="production" value="production"/>
      </el-select>
      <el-input v-model="module" class="ml-5" style="width: 200px" placeholder="请输入服务名称" suffix-icon="el-icon-search" />
      <el-button class="ml-5" type="primary" @click="fetchData">搜索</el-button>
      <el-button type="warning" @click="reset">重置</el-button>
    </div>
    <div style="margin: 10px 0">
      <el-button type="primary" @click="handleAdd">新增 <i class="el-icon-circle-plus-outline" /></el-button>
      <el-popconfirm
        class="ml-5"
        confirm-button-text="确定"
        cancel-button-text="我再想想"
        icon="el-icon-info"
        icon-color="red"
        title="您确定删除这些数据吗？"
        @onConfirm="delBatch"
      >
        <el-button slot="reference" type="danger" :disabled="!multipleSelection.length">批量删除 <i class="el-icon-remove-outline" /></el-button>
      </el-popconfirm>
    </div>
    <el-table
      v-loading="listLoading"
      :data="tableData.slice((pageNum - 1) * pageSize, (pageNum - 1) * pageSize + pageSize)"
      element-loading-text="Loading"
      border
      highlight-current-row
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column align="center" label="序号" width="95">
        <template slot-scope="scope">
          {{ scope.$index }}
        </template>
      </el-table-column>
      <el-table-column label="服务" align="center">
        <template slot-scope="scope">
          <el-tag type="danger">{{ scope.row.module }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否开启" align="center">
        <template slot-scope="scope">
          <el-tag type="success">{{ scope.row.enable }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="限流类型" align="center">
        <template slot-scope="scope">
          <el-tag type="warning">{{ scope.row.types }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template slot-scope="scope">
          <el-button type="success" @click="handleEdit(scope.row)">编辑 <i class="el-icon-edit" /></el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="padding: 10px 0">
      <el-pagination
        :current-page="pageNum"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="tableData.length"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    <el-dialog title="编辑限流服务" :visible.sync="dialogFormVisible" width="30%">
      <el-form label-width="120px" size="small">
        <el-form-item label="服务">
          <el-input v-model="form.module" :disabled="editTypeDisabled" autocomplete="off" />
        </el-form-item>
        <el-form-item label="是否开启限流">
          <el-switch
            v-model="form.enable"
            active-text="开 启"
            inactive-text="关 闭">
          </el-switch>
        </el-form-item>
        <el-form-item label="限流类型">
          <el-select multiple clearable v-model="form.types" style="width: 100%">
            <el-option label="按服务QPS" value="qps" />
            <el-option label="按IP" value="ip" />
            <el-option label="按指定接口" value="interface" />
            <el-option label="按服务错误CODE码" value="error_code_num" />
            <el-option label="按热点参数" value="hot" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveOrUpdate">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import ratelimitApi from '@/api/ratelimit'

export default {
  filters: {
    statusFilter(status) {
      const statusMap = {
        published: 'success',
        draft: 'gray',
        deleted: 'danger'
      }
      return statusMap[status]
    }
  },
  data() {
    return {
      tableData: [],
      listLoading: true,
      multipleSelection: [],
      dialogFormVisible: false,
      editTypeDisabled: false,
      module: '',
      env: 'release',
      form: {
        module: '',
        enable: true,
        types: ['qps']
      },
      pageNum: 1,
      pageSize: 10
    }
  },
  // created() {
  //   this.fetchData()
  // },
  methods: {
    fetchData() {
      this.listLoading = true
      ratelimitApi.loadRateLimitSwitch({
        module: this.module,
        env: this.env
      }).then(response => {
        this.tableData = response.data || []
        this.listLoading = false
      })
    },
    reset() {
      this.module = ''
      this.env = 'default'
      this.fetchData()
    },
    handleSelectionChange(val) {
      this.multipleSelection = val
    },
    handleAdd() {
      this.dialogFormVisible = true
      this.form = {
        module: '',
        enable: true,
        types: ['qps'],
        saveOrNot: true
      }
      this.editTypeDisabled = false
    },
    delBatch() {
      // [{}, {}, {}] => [1,2,3] => 1,2,3
      const ids = this.multipleSelection.map(v => v.module).toString()
      ratelimitApi.delRateLimitSwitch({ ids, env: this.env }).then(res => {
        if (res.status === 200) {
          this.$message.success('删除完毕')
          this.fetchData()
        } else {
          this.$message.error(`删除失败: ${res.statusText}`)
        }
      })
    },
    saveOrUpdate() {
      ratelimitApi.saveOrUpdateRateLimitSwitch({ ...this.form, env: this.env }).then(res => {
        if (res.status === 200) {
          this.fetchData()
          this.$message.success('操作成功')
          this.dialogFormVisible = false
        } else {
          this.$message.error(`操作失败: ${res.statusText}`)
        }
      })
    },
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row))
      this.dialogFormVisible = true
      this.form.saveOrNot = false
      this.editTypeDisabled = true
    },
    handleSizeChange(pageSize) {
      this.pageSize = pageSize
    },
    handleCurrentChange(pageNum) {
      this.pageNum = pageNum
    }
  }
}
</script>
<style lang="scss" scoped>
.ml-5 {
  margin-left: 5px;
}
</style>
