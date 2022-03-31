import {BasicColumn} from '/@/components/Table';
import {FormSchema} from '/@/components/Table';
import { rules} from '/@/utils/helper/validator';
import { render } from '/@/utils/common/renderUtils';

export const columns: BasicColumn[] = [
    {
    title: '招聘发布信息用户',
    dataIndex: 'userid'
   },
   {
    title: '发布内容',
    dataIndex: 'context'
   },
   {
    title: '头像',
    dataIndex: 'avatar'
   },
];

export const searchFormSchema: FormSchema[] = [
 {
    label: '招聘发布信息用户',
    field: 'userid',
    component: 'Input'
  },
 {
    label: '发布内容',
    field: 'context',
    component: 'Input'
  },
];

export const formSchema: FormSchema[] = [
  {
    label: '招聘发布信息用户',
    field: 'userid',
    component: 'Input'
  },
  {
    label: '发布内容',
    field: 'context',
    component: 'Input'
  },
  {
    label: '头像',
    field: 'avatar',
    component: 'Input'
  },
];
