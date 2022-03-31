import {BasicColumn} from '/@/components/Table';
import {FormSchema} from '/@/components/Table';
import { rules} from '/@/utils/helper/validator';
import { render } from '/@/utils/common/renderUtils';

export const columns: BasicColumn[] = [
    {
    title: '消息内容',
    dataIndex: 'context'
   },
   {
    title: '发留言用户id可判断是否为本人',
    dataIndex: 'userid'
   },
   {
    title: '区分身份信息',
    dataIndex: 'identity'
   },
   {
    title: '可见性0本人可见 1全体 -1删除 ',
    dataIndex: 'visible'
   },
   {
    title: '头像',
    dataIndex: 'avatar'
   },
];

export const searchFormSchema: FormSchema[] = [
 {
    label: '消息内容',
    field: 'context',
    component: 'Input'
  },
 {
    label: '发留言用户id可判断是否为本人',
    field: 'userid',
    component: 'Input'
  },
];

export const formSchema: FormSchema[] = [
  {
    label: '消息内容',
    field: 'context',
    component: 'Input'
  },
  {
    label: '发留言用户id可判断是否为本人',
    field: 'userid',
    component: 'Input'
  },
  {
    label: '区分身份信息',
    field: 'identity',
    component: 'InputNumber'
  },
  {
    label: '可见性0本人可见 1全体 -1删除 ',
    field: 'visible',
    component: 'InputNumber'
  },
  {
    label: '头像',
    field: 'avatar',
    component: 'Input'
  },
];
